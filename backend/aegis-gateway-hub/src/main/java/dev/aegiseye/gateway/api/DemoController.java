package dev.aegiseye.gateway.api;

import dev.aegiseye.gateway.demo.DemoService;
import dev.aegiseye.gateway.demo.DemoTamperRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping("/tamper")
    public Map<String, Object> tamper(@RequestBody DemoTamperRequest request) {
        return demoService.tamper(request);
    }
}
