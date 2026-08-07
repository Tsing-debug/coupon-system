package com.ali.coupon.controller;

import com.ali.coupon.dto.ExchangeRequest;
import com.ali.coupon.dto.ExchangeResult;
import com.ali.coupon.service.ExchangeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    @PostMapping
    public ExchangeResult exchange(@Valid @RequestBody ExchangeRequest request) {
        return exchangeService.exchange(
                request.getUserId(),
                request.getActivityId(),
                request.getTemplateId(),
                "SHOP001",
                "10.00"
        );
    }
}
