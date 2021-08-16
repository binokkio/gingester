List:
<#list testList as item>
- ${item}
</#list>

Hash:
<#list testHash as key, value>
- ${key}: ${value}
</#list>