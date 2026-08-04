package org.janan.controller;


import jakarta.validation.constraints.NotBlank;
import org.janan.dto.AskResponse;
import org.janan.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService){
        this.queryService = queryService;
    }

    @GetMapping("/ask")
    public ResponseEntity<AskResponse> ask(
            @RequestParam("q") @NotBlank(message = "Question must not be blank") String q) {
        return ResponseEntity.ok(queryService.ask(q));
    }


}
