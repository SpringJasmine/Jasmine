package utils;

import bean.AopXMLResultBean;
import bean.ConstructorArgBean;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName   XMLDocumentHolder
 * @Description the parser of XML document
 */
public class XMLDocumentHolder {

    //Create a HashMap to store strings and documents
    private Map<String, Document> docs = new HashMap<String, Document>();
    private Map<String, Element> elements = new HashMap<>();
    private List<ConstructorArgBean> argConstructors = new ArrayList<>();

    public Document getDocument(String filePath) {
        // Use HashMap to get the document according to the path
        Document doc = this.docs.get(filePath);
        if (doc == null) {
            this.docs.put(filePath, readDocument(filePath));
        }
        return this.docs.get(filePath);
    }

    /**
     * read document according to path
     *
     * @param filePath file path
     * @return Document
     */
    private Document readDocument(String filePath) {
        Document doc = null;
        try {
            SAXReader reader = new SAXReader();
            File xmlFile = new File(filePath);
            if (xmlFile.exists()) {
                doc = reader.read(xmlFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }


    public void addElements(Document doc) {
        @SuppressWarnings("unchecked")
        List<Element> eles = doc.getRootElement().elements("bean");
        for (Element e : eles) {
            String id = e.attributeValue("id");
            elements.put(id, e);
        }
    }


    public String getCustomMapperAnnotationClazz(Document doc) {
        List<Element> eles = doc.getRootElement().elements("bean");
        for (Element ele : eles) {
            if ("org.mybatis.spring.mapper.MapperScannerConfigurer".equals(ele.attribute("class").getValue())) {
                List<Element> properties = ele.elements("property");
                for (Element property : properties) {
                    if ("annotationClass".equals(property.attribute("name").getValue())) {
                        return property.attribute("value").getValue();
                    }
                }
            }
        }
        return "";
    }


    /**
     * Analyze and construct injected beans with parameters
     *
     * @param doc
     */
    public void hasArgConstructorBean(Document doc) {
        List<Element> eles = doc.getRootElement().elements("bean");
        for (Element ele : eles) {
            List<Element> elelist = ele.elements("constructor-arg");
            if (elelist != null && elelist.size() > 0) {
                String id = ele.attributeValue("id");

                for (Element element : elelist) {
                    ConstructorArgBean argBean = new ConstructorArgBean();
                    argBean.setClazzName(ele.attribute("class").getText());
                    if (element.attribute("name") != null) {
                        argBean.setArgName(element.attribute("name").getText());
                    }
                    if (element.attribute("index") != null) {
                        argBean.setArgIndex(Integer.valueOf(element.attribute("index").getText()));
                    }
                    if (element.attribute("type") != null) {
                        argBean.setArgType(element.attribute("type").getText());
                    }
                    if (element.attribute("ref") != null) {
                        String ref = element.attribute("ref").getText();
                        Element refelem = elements.get(ref);
                        String aClass = refelem.attribute("class").getText();
                        argBean.setRefType(aClass);
                    }
                    if (element.attribute("value") != null) {
                        argBean.setArgValue(element.attribute("value").getText());
                    } else {
                        Element value = element.element("value");
                        if (value != null) {
                            argBean.setArgValue(value.getTextTrim());
                        }
                    }
                    argConstructors.add(argBean);
                }
            }
        }
    }

    public Map<String, String> getAllClassMap() {
        Map<String, String> map = new HashMap<>();
        for (String s : elements.keySet()) {
            Element element = elements.get(s);

            String aClass = element.attribute("class").getText();
            if (element.attribute("scope") == null) {
                aClass += ";singleton";
            } else {
                aClass += ";" + element.attribute("scope").getText();
            }
            map.put(s, aClass);
        }
        return map;
    }

    public List<ConstructorArgBean> getArgConstructors() {
        return argConstructors;
    }


    /**
     * Process AOP related configuration in xml
     *
     * @param doc the doc of xml file
     * @return the list of AopXMLResultBean
     */
    public List<AopXMLResultBean> processAopElements(Document doc) {
        List<Element> configlist = doc.getRootElement().elements("config");
        List<Element> pointcutelements = new ArrayList<>();
        List<Element> partpointcutele = new ArrayList<>();
        List<Element> aspelement = new ArrayList<>();
        List<Element> beforeelement = new ArrayList<>();
        List<Element> afterelement = new ArrayList<>();
        List<Element> aroundelement = new ArrayList<>();
        List<Element> areturnelement = new ArrayList<>();
        List<Element> athrowelement = new ArrayList<>();

        Map<String, String> partpointmap = new HashMap<>();
        Map<String, String> stringStringMap = new HashMap<>();
        Map<String, String> tmppointmap = new HashMap<>();


        List<AopXMLResultBean> resbenas = new ArrayList<>();
        int gorder = 1;
        for (Element config : configlist) {
            aspelement.clear();
            stringStringMap.clear();
            pointcutelements.addAll(config.elements("pointcut"));
            if (pointcutelements.size() > 0) {
                stringStringMap.putAll(pointcutelesProcess(pointcutelements));
            }
            aspelement.addAll(config.elements("aspect"));
            for (Element asp : aspelement) {
                partpointmap.clear();
                tmppointmap.clear();
                beforeelement.clear();
                afterelement.clear();
                aroundelement.clear();
                areturnelement.clear();
                athrowelement.clear();
                String id = asp.attribute("ref").getText();
                int order = 1;
                if (asp.attribute("order") != null) {
                    order = Integer.parseInt(asp.attribute("order").getText());
                } else {
                    order = gorder++;
                }
                Element element = elements.get(id);
                String aClass = element.attribute("class").getText();


                partpointcutele.addAll(asp.elements("pointcut"));
                if (partpointcutele.size() > 0) {
                    partpointmap.putAll(pointcutelesProcess(partpointcutele));
                }
                tmppointmap.putAll(stringStringMap);
                tmppointmap.putAll(partpointmap);

                beforeelement.addAll(asp.elements("before"));
                Map<String, String> beforemap = alertProcess(beforeelement);
                resbenas.addAll(ProcessActiveAndPointcut(tmppointmap, beforemap, aClass, "before", order));

                afterelement.addAll(asp.elements("after"));
                Map<String, String> aftermap = alertProcess(afterelement);
                resbenas.addAll(ProcessActiveAndPointcut(tmppointmap, aftermap, aClass, "after", order));

                aroundelement.addAll(asp.elements("around"));
                Map<String, String> aroundmap = alertProcess(aroundelement);
                resbenas.addAll(ProcessActiveAndPointcut(tmppointmap, aroundmap, aClass, "around", order));

                areturnelement.addAll(asp.elements("after-returning"));
                Map<String, String> areturnmap = alertProcess(areturnelement);
                resbenas.addAll(ProcessActiveAndPointcut(tmppointmap, areturnmap, aClass, "afterreturn", order));

                athrowelement.addAll(asp.elements("after-throwing"));
                Map<String, String> athrowmap = alertProcess(athrowelement);
                resbenas.addAll(ProcessActiveAndPointcut(tmppointmap, athrowmap, aClass, "afterthrow", order));
            }
        }

        return resbenas;
    }

    /**
     * Correlate the advices with pointcut expression
     *
     * @param pointcutmap pointcut map
     * @param activemap   advice map
     * @param aclazz      the string of aspect
     * @param activetype  active type
     * @return the list of AopXMLResultBean
     */
    public List<AopXMLResultBean> ProcessActiveAndPointcut(Map<String, String> pointcutmap, Map<String, String> activemap, String aclazz, String activetype, int order) {
        List<AopXMLResultBean> beanList = new ArrayList<>();
        if (pointcutmap.size() > 0 && activemap.size() > 0) {
            for (String value : activemap.values()) {
                String method = value.split(";")[0];
                String pcutref = value.split(";")[1];
                for (String s : pointcutmap.keySet()) {
                    if (pcutref.equals(s)) {
                        AopXMLResultBean aopXMLResultBean = new AopXMLResultBean();
                        aopXMLResultBean.setAopclass(aclazz);
                        aopXMLResultBean.setAopmethod(method);
                        aopXMLResultBean.setActivetype(activetype);
                        aopXMLResultBean.setExper(pointcutmap.get(s));
                        aopXMLResultBean.setOrder(order);
                        beanList.add(aopXMLResultBean);
                    }
                }
            }
        }
        return beanList;
    }

    /**
     * the map of key is id and val is expression
     *
     * @param pointcutelements the list of pointcut label
     * @return the map of key is id and val is expression
     */
    public Map<String, String> pointcutelesProcess(List<Element> pointcutelements) {
        Map<String, String> res = new HashMap<>();
        for (Element pointcutelement : pointcutelements) {
            String id = pointcutelement.attribute("id").getText();
            String expression = pointcutelement.attribute("expression").getText();
            res.put(id, expression);
        }
        return res;
    }

    /**
     * Process the advice label and get pointcut-ref
     *
     * @param alertelement the list of alert element
     * @return  the map of key is methodName+pointcut and val is pointcut id
     */
    public Map<String, String> alertProcess(List<Element> alertelement) {
        Map<String, String> res = new HashMap<>();
        if (alertelement.size() > 0) {
            for (Element element : alertelement) {
                String method = element.attribute("method").getText();
                String pintcutref = element.attribute("pointcut-ref").getText();
                res.put(method + pintcutref, method + ";" + pintcutref);
            }
        }
        return res;
    }

    public List getChildElements(Element element) {
        List elements = element.elements();
        return elements;
    }

    public Element getElement(String id) {
        return elements.get(id);
    }

}
