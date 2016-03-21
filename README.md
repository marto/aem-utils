# aem-utils
Common AEM / Sling Utils

Provide common utility functions and helper classes to ease development in Adobe AEM / Apache Sling. Licenced under LGPL 3

### FreemarkerTemplateFactory
Load and render freemarker templates that are stored as resources in your OSGi files

### FreemarkerTemplatedMailer
Send freemarker templated HTML emails

### Vanity URL Filter
Enables vanity paths to be hosted under certain root paths (eg: /content/geometrix) and to ease dispatcher configuration & access filter setup.

The recommended approach when setting up an AEM dispatcher is to secure the paths accessible to the back-end AEM instance, by first blacklisting all paths and then only allowing individual content paths through (eg ```/content/geometrix/*```). Unfortunately, when using the out of the box Vanity Path functionality this allows a content author to setup vanity paths such as "vanity-path" (i.e. under root) and the dispatcher finds it rather difficult to determine if a request is a vanity path or not. One way around this is to always add the contentRoot to all paths to the apache dispatcher reverse proxyso "/vanity-path" is rewritten to "/content/geometrix/vanity-path" before it reaches the publisher. However, "/content/geometrix/vanity-path" will not resolve unless the vanity path is specifically defined that way. The "Vanity URL Filter" allows content authors to specify vanity URLs without the contetRoot (eg ```/cotnent/geometix"``` prefix). This filter allows those vanity URL to be prefixed with the content root when they come into AEM and the request is internally forwarded if and only if the request resolves to a vanity URL that falls under one of the allowed content roots.

It should be noted that when using global vanity paths in a multi-tenanted system (i.e. not prefixing them with a unique root content path), it's very likely that you will get clashes.

To use the Vanity URL Filter, simply add the dependancy to your POM and configure the "Vanity URL Filter" component.

```
  <dependencies>
    <dependency>
      <groupId>io.marto.aem</groupId>
      <artifactId>aem-vanity-filter</artifactId>
      <version>0.0.5</version>
    </dependency>
  </dependencies>

```

### License

LGPL 3 - See LICENSE.txt
