package com.erencsahin.calculator.impl;

import com.erencsahin.dto.Rate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class JavaScriptRateCalculator {
    private final ScriptEngine engine;
    @Autowired ResourceLoader resourceLoader;

    public JavaScriptRateCalculator() {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        if (engine == null) {
            throw new IllegalStateException("Nashorn ScriptEngine bulunamadı");
        }
    }

    public Rate calculateFromFile(String scriptName, Map<String, Rate> vars) {
        try {
            Resource res = resourceLoader.getResource("classpath:formulas/" + scriptName + ".js");
            String js = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            Bindings bindings = engine.createBindings();
            bindings.putAll(vars);

            Object result = engine.eval(js, bindings);
            return (Rate) result;
        } catch (ScriptException | java.io.IOException ex) {
            throw new IllegalStateException("JS script çalıştırılamadı: " + scriptName, ex);
        }
    }
}
