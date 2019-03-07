package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * demo
 * Created by zjq on 2019/3/7.
 */
@Controller
public class DemoController {
    @RequestMapping("/index")
    @ResponseBody
    public String index(){
        return "index";
    }

    @RequestMapping("/")
    public String demo(){
        return "demo";
    }
}
