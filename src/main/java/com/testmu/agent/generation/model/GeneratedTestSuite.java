package com.testmu.agent.generation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedTestSuite {

    private String module;
    private String className;
    private String baseClass;
    private List<String> imports = new ArrayList<>();
    private List<GeneratedTestCase> tests = new ArrayList<>();

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getBaseClass() {
        return baseClass;
    }

    public void setBaseClass(String baseClass) {
        this.baseClass = baseClass;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports != null ? imports : new ArrayList<>();
    }

    public List<GeneratedTestCase> getTests() {
        return tests;
    }

    public void setTests(List<GeneratedTestCase> tests) {
        this.tests = tests != null ? tests : new ArrayList<>();
    }
}
