package com.fortify.cli.ssc.picocli.mixin.plugin.parser;

import picocli.CommandLine;
import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;

public class SSCParserPluginSelectorMixin {
    @CommandLine.ArgGroup(heading = "Identifier\n")
    private Identifier identifier;

    static class Identifier{
        @CommandLine.Option(names = {"--id"})
        private Integer id;

    }
    @CommandLine.ArgGroup(heading = "Selection Criteria\n")
    private Selectors selectors;

    static class Selectors{
        @CommandLine.Option(names = {"--engineType"})
        public String engineType;

        @CommandLine.Option(names = {"--pluginId"})
        public String pluginId;

        @CommandLine.Option(names = {"--pluginState"})
        public String pluginState;

        @CommandLine.Option(names = {"--pluginName"})
        public String pluginName;

        @CommandLine.Option(names = {"--pluginVersion"})
        public String pluginVersion;

    }

    private void exclusivityTest(){
        if(identifier != null && selectors != null)
            throw new ValidationException("You can not use the --id option with any of the specifier options");
    }

    public String getSelectorJsonPathQuery(){
        exclusivityTest();

        if(identifier == null && selectors == null)
            return ".*";

        if(identifier != null){
            return String.format("$.data[?(@.id == \"%s\")]", identifier.id.toString());
        }

        Map<String,String> selectorList = new HashMap<String,String>();
        selectorList.put("engineType",selectors.engineType);
        selectorList.put("pluginId",selectors.pluginId);
        selectorList.put("pluginName",selectors.pluginName);
        selectorList.put("pluginState",selectors.pluginState);
        selectorList.put("pluginVersion",selectors.pluginVersion);

        String part = "";
        for (Map.Entry<String,String> e : selectorList.entrySet()){
            if(e.getValue() == null)
                continue;
            if(!part.isEmpty())
                part += " &&";
            part += String.format(" @.%s == \"%s\"", e.getKey(), e.getValue());
        }

        return String.format("$.data[?(%s)]", part);
    }
        public boolean isSelectorSpecified(){
            if(identifier != null || selectors != null)
                return true;
            return false;
        }

}
