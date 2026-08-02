# DocQuery Assist

A small RAG (Retrieval-Augmented Generation) service that lets you upload a document and ask questions about it. The answers are grounded in the document itself — if something isn't in there, it says so instead of making something up.

Built for the Seeburger second-round assignment.

## What it does

Upload a PDF or text file, then ask questions about it in plain English. The service finds the most relevant parts of the document, hands them to an LLM along with your question, and returns an answer plus the exact passages it was based on. If nothing in the document is actually relevant to your question, you get a clear "couldn't find anything relevant" response rather than a confident-sounding guess.

## How it's put together

```
Upload:  DocumentController -> IngestionService -> TextExtractor -> ChunkingService -> EmbeddingClient -> Postgres/pgvector
Ask:     QueryController -> QueryService -> RetrievalService -> PromptBuilder -> GenerationClient -> answer + sources
```

I kept this intentionally flat. It would have been easy to add a `RetrievalCoordinator` or an `EmbeddingOrchestrator` on top, but that just adds indirection without adding value for a project this size — every class here exists because a specific step in the pipeline needed it, and nothing more.

## Why these tools

**Postgres + pgvector** for the vector store, instead of a separate vector database. It means one thing to run, and the similarity search is just a SQL query I can write and read myself, rather than a new client SDK to learn. I run it on Neon (hosted Postgres with pgvector support) rather than locally, mainly because of environment issues on my machine during development — it works exactly the same either way, just a different connection string.

**Google's Gemini API for both embeddings and generation.** Originally I'd planned to split this across OpenAI (embeddings) and Claude (generation), since Anthropic doesn't expose an embeddings endpoint. Partway through I decided to keep this project entirely free to run — Gemini has a genuine free tier for both embeddings and text generation, with no billing setup needed, so I moved both pieces onto it. One vendor, zero cost, and honestly one fewer set of API keys to juggle.

**Apache Tika** for pulling text out of PDFs/text files. It wraps PDFBox under the hood but handles more formats consistently, and PDF text extraction is messier than people expect — sometimes words run together, sometimes there are blank lines everywhere from page breaks. I do a basic whitespace cleanup pass on whatever Tika gives back before it goes anywhere near the chunker.

**Plain Spring `RestClient`** for the Gemini calls, rather than pulling in a vendor SDK. Fewer dependencies, and every request and response is something I wrote and can point to directly.

## Chunking

Chunks are roughly 800 characters, with 15% overlap between consecutive chunks, and the splitter tries to break on a paragraph or sentence boundary rather than mid-word if there's one nearby.

I went back and forth on whether to do "real" tokenizer-aware chunking (i.e., actually count tokens the way the embedding model would). Getting this exactly right would mean tracking the specific tokenizer for whichever model is in use — a lot of effort for something where an approximation works fine. 800 characters lands around 400-500 tokens for normal English text, which is close enough, and it's a much simpler piece of code to maintain and explain.

The overlap exists because without it, a fact that happens to sit right at a chunk boundary can end up split across two chunks, and neither half reads as a strong match for a question about it.

Both the chunk size and the overlap percentage live in `application.yml`, not hardcoded — easy to tune without touching code.

## Keeping answers honest

This is the part I spent the most time thinking about, since it's really the whole point of doing RAG instead of just calling an LLM directly.

When a question comes in, I embed it, run a similarity search against the stored chunks (top 3), and then check the similarity score against a threshold. If nothing clears that bar, the API just returns a 422 with a clear message — it doesn't try to force an answer out of whatever weak matches it found.

The threshold is set at **0.60**, and getting to that number is a decent example of why I didn't want this hardcoded from the start. I originally set it at 0.80 as a reasonable-sounding guess. Once I actually ran real questions against real embeddings, genuinely relevant matches were consistently scoring around 0.62–0.65 — Gemini's embedding model just produces a different score distribution than what I'd assumed, and 0.80 would have rejected almost everything, including good matches. I lowered it based on what I actually observed, not a formula. It's a config value specifically so this kind of adjustment doesn't need a code change.

For the chunks that do pass, the prompt sent to the model is explicit: answer only from what's given, and say so plainly if the answer isn't there rather than guessing. There's also a line in there addressing something that's easy to forget — the retrieved text came from a document someone uploaded, which makes it untrusted input. The system prompt tells the model to treat that content as reference material only and not follow any instructions that might be sitting inside it.

One more small thing: the API returns a `similarityScore` for each source, not a "confidence" number. A model doesn't actually know how confident it should be — that's not a value it can meaningfully produce — so I didn't want to invent one. A cosine similarity score, on the other hand, is something concrete I can point to and explain.

## The actual vector search

I wrote this as a plain native SQL query rather than hiding it behind a generic repository method, mostly because I think it's worth being able to show:

```sql
SELECT * FROM chunks c
ORDER BY c.embedding <=> CAST(:embedding AS vector)
LIMIT :topK
```

`<=>` is pgvector's cosine distance operator — smaller means more similar. I compute the actual similarity score (`1 - distance`) back in Java rather than in SQL, mainly so that bit of math lives in a small, plain function I can unit test without needing a database at all.

## Running it locally

You'll need JDK 21+, Maven, Docker (for the Testcontainers integration test — the app itself can run against any reachable Postgres+pgvector instance), and a Gemini API key.

```bash
git clone <repo-url>
cd doc-query-assist

docker compose up -d          # starts Postgres with pgvector, if running locally

export GEMINI_API_KEY=your-key-here

mvn spring-boot:run
```

It comes up on `localhost:8080`. `/actuator/health` if you want to confirm it's alive (this also confirms the database connection is actually working, not just that the app booted).

Get a free Gemini key at https://aistudio.google.com/apikey — no credit card needed.

## Trying it out

Uploading something:
```bash
curl -X POST http://localhost:8080/api/documents/upload -F "file=@sample.txt"
```
```json
{
  "documentId": "1380f562-2918-45de-896d-b1713e50949e",
  "fileName": "sample.txt",
  "status": "PROCESSED"
}
```

Asking about it:
```bash
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the refund policy?"}'
```
```json
{
  "question": "What is the refund policy?",
  "answer": "Our refund policy allows customers to return any unused item within 30 days of purchase, provided it is in its original packaging with proof of purchase. Refunds are processed within 5 to 7 business days after the returned item is received and inspected. Sale items and gift cards are non-refundable.",
  "sources": [
    { "document": "sample.txt", "chunkId": "8755f3c6-996c-4c8c-8137-8aaa3fa4a7e1", "score": 0.65 },
    { "document": "sample.txt", "chunkId": "a3dce8be-6472-490d-bccd-1d012b5cbac1", "score": 0.62 }
  ]
}
```

And when there's genuinely nothing relevant:
```bash
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the company'\''s stock price?"}'
```
```json
{
  "error": "NO_RELEVANT_CHUNK",
  "message": "No chunk met the similarity threshold of 0.6 for the given query.",
  "status": 422,
  "timeStamp": "2026-08-02T19:27:36.897509700Z"
}
```

## Testing

I didn't aim for a coverage number — I tried to cover the things that are actually easy to get wrong:

- Chunking: empty documents, documents smaller than one chunk, whether overlap actually overlaps, whether paragraph boundaries get respected, and a guard against a misconfigured overlap value causing an infinite loop
- The similarity math: identical vectors, orthogonal vectors, opposite vectors, mismatched dimensions, the zero-vector edge case
- Prompt building: that the refusal instruction and the injection-mitigation line are actually present, that chunk ordering is preserved
- One integration test end to end (upload -> ask -> answer) against a real Postgres+pgvector instance via Testcontainers, rather than mocking the database

The chunking, vector math, and prompt tests don't need Docker or a database at all:
```bash
mvn test -Dtest=ChunkingServiceTest,VectorUtilsTest,PromptBuilderTest
```

Full suite, including the Testcontainers integration test:
```bash
mvn test
```

A small note on the logging: `RetrievalService` logs each candidate chunk's similarity score at INFO level before the threshold filter runs. I added this while debugging the threshold value above and decided to leave it in rather than strip it out — it's genuinely useful for seeing what the retrieval step is actually doing, and it's exactly the kind of thing worth having on hand if a similar tuning question comes up again later.

## What I'd add next

- **Async ingestion with Kafka** — publish an event on upload and have a worker handle chunking/embedding in the background instead of doing it all inline in the request. I thought about building this in, but decided a half-built async pipeline is worse than a working synchronous one given the time I had — this is a clearly-scoped next step, not something I wanted to rush.
- **Caching embeddings** by hashing the document content, so re-uploading the same file doesn't burn API calls a second time.
- **Actual token-based chunking**, if it turns out the character approximation isn't good enough in practice.
- **Basic timing/logging** on the upload and ask endpoints, so there's some observability into where time is actually going.
