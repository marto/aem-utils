# Vanity URL Filter

The recommended approach when setting up an AEM dispatcher is to secure the paths accessible to the back-end AEM instance, by first blacklisting all paths and then only allowing individual content paths through (eg ```/content/geometrix/*```). Unfortunately, when using the out of the box Vanity Path functionality this allows a content author to setup vanity paths such as "vanity-path" (i.e. under root) and the dispatcher finds it rather difficult to determine if a request is a vanity path or not. One way around this is to always add the contentRoot to all paths so "/Vanity-Path" is rewritten to "/content/geometrix/Vanity-Path" when it reaches the publisher. However, "/content/geometrix/Vanity-Path" will not resolve unless the vanity path is specifically defined that way. The "Vanity URL Filter" allows content authors to specify vanity URLs without the contetRoot (eg ```/cotnent/geometix"``` prefix). This filter allows those vanity URL to be prefixed with the content root when they come into AEM and the request is internally forwarded if and only if the request resolves to a vanity URL that falls under one of the allowed content roots.


```
  
```
