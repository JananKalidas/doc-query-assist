package org.janan.controller;

import jakarta.validation.Valid;
import org.janan.dto.AskRequest;
import org.janan.dto.AskResponse;
import org.janan.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService){
        this.queryService = queryService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(
            @Valid @RequestBody AskRequest request
            ){
        return ResponseEntity.ok(
                queryService.ask(request.question())
        );
    }


}
