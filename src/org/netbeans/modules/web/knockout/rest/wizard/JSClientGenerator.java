package org.netbeans.modules.web.knockout.rest.wizard;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.web.knockout.rest.wizard.RestPanel.JsUi;
import org.netbeans.modules.websvc.rest.model.api.RestServiceDescription;
import org.openide.filesystems.FileObject;

public class JSClientGenerator {

    enum MethodType {
        GET,
        SET
    }

    enum HttpRequests {

        POST("create"),
        PUT("update"),
        DELETE("delete");

        HttpRequests(String method) {
            myBackboneMethod = method;
        }

        @Override
        public String toString() {
            return myBackboneMethod;
        }

        private String myBackboneMethod;
    }

    private static final Logger LOG = Logger.getLogger(JSClientGenerator.class.getName());

    private static final String PATH = "javax.ws.rs.Path";
    private static final String PATH_PARAM = "javax.ws.rs.PathParam";
    private static final String GET = "javax.ws.rs.GET";
    private static final String PUT = "javax.ws.rs.PUT";
    private static final String POST = "javax.ws.rs.POST";
    private static final String DELETE = "javax.ws.rs.DELETE";
    private static final String PRODUCES = "javax.ws.rs.Produces";
    private static final String CONSUMES = "javax.ws.rs.Consumes";
    private static final String JSON = "application/json";

    private static final String XML_ROOT_ELEMENT
            = "javax.xml.bind.annotation.XmlRootElement";

    private JSClientGenerator(RestServiceDescription description, JsUi ui) {
        myDescription = description;
        myUi = ui;
    }

    public static JSClientGenerator create(RestServiceDescription description, JsUi ui) {
        return new JSClientGenerator(description, ui);
    }

    public Map<String, String> generate() {
        FileObject restSource = myDescription.getFile();
        if (restSource == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<String, String>();
        myModels = new StringBuilder();
        myRouters = new StringBuilder();
        myContent = new StringBuilder();
        myHeader = new StringBuilder();
        mySidebar = new StringBuilder();
        myTmplCreate = new StringBuilder();
        myTmplList = new StringBuilder();;
        myTmplDetails = new StringBuilder();

        JavaSource javaSource = JavaSource.forFileObject(restSource);
        final String restClass = myDescription.getClassName();
        Task<CompilationController> task = new Task<CompilationController>() {

            @Override
            public void run(CompilationController controller) throws Exception {
                controller.toPhase(Phase.ELEMENTS_RESOLVED);

                List<ExecutableElement> getMethods = new LinkedList<ExecutableElement>();
                List<ExecutableElement> postMethods = new LinkedList<ExecutableElement>();
                List<ExecutableElement> putMethods = new LinkedList<ExecutableElement>();
                List<ExecutableElement> deleteMethods = new LinkedList<ExecutableElement>();

                TypeElement restResource = controller.getElements().getTypeElement(
                        restClass);
                List<ExecutableElement> methods = ElementFilter.methodsIn(
                        restResource.getEnclosedElements());
                for (ExecutableElement method : methods) {
                    List<? extends AnnotationMirror> annotations
                            = method.getAnnotationMirrors();
                    if (getAnnotation(annotations, DELETE) != null) {
                        deleteMethods.add(method);
                        continue;
                    }
                    if (!hasJsonMedia(annotations)) {
                        continue;
                    }
                    if (getAnnotation(annotations, GET) != null) {
                        getMethods.add(method);
                    } else if (getAnnotation(annotations, POST) != null) {
                        postMethods.add(method);
                    } else if (getAnnotation(annotations, PUT) != null) {
                        putMethods.add(method);
                    }
                }

                try {
                    handleRestMethods(controller, getMethods, postMethods,
                            putMethods, deleteMethods);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, null, e);
                }
            }

        };
        try {
            Future<Void> future = javaSource.runWhenScanFinished(task, true);
            future.get();
        } catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        } catch (InterruptedException e) {
            LOG.log(Level.INFO, null, e);
        } catch (ExecutionException e) {
            LOG.log(Level.INFO, null, e);
        }

        if (!isModelGenerated) {
            myModels.append("// No JSON media type is detected in GET RESTful methods\n");
        }
        result.put("models", myModels.toString());
        result.put("routers", myRouters.toString());
        result.put("header", myHeader.toString());
        result.put("sidebar", mySidebar.toString());
        result.put("content", myContent.toString());
        result.put("tpl_create", myTmplCreate.toString());
        result.put("tpl_list_item", myTmplList.toString());
        result.put("tpl_details", myTmplDetails.toString());
        if (hasUi()) {
            result.put("ui", Boolean.TRUE.toString());
        }

        return result;
    }

    boolean hasUi() {
        return hasUi;
    }

    private void handleRestMethods(CompilationController controller,
            List<ExecutableElement> getMethods,
            List<ExecutableElement> postMethods,
            List<ExecutableElement> putMethods,
            List<ExecutableElement> deleteMethods) throws IOException {
        Map<String, ExecutableElement> noParamGetMethods
                = new HashMap<String, ExecutableElement>();
        Map<String, ExecutableElement> oneParamGetMethods
                = new HashMap<String, ExecutableElement>();
        for (ExecutableElement method : getMethods) {
            List<? extends VariableElement> parameters = method.getParameters();
            if (parameters.size() > 1) {
                // TODO : handle methods with more than one  param
                continue;
            }

            AnnotationMirror annotation = getAnnotation(method, PATH);
            String path = getValue(annotation);
            if (parameters.isEmpty()) {
                if (path == null) {
                    path = "";
                }
                noParamGetMethods.put(path, method);
                continue;
            }

            VariableElement param = parameters.get(0);
            annotation = getAnnotation(param, PATH_PARAM);
            if (annotation == null) {
                continue;
            }
            String pathNoParam = removeParamTemplate(path, getValue(annotation));
            oneParamGetMethods.put(pathNoParam, method);
        }
        Map<String, String> fqn2Path = new HashMap<String, String>();
        for (Entry<String, ExecutableElement> entry : noParamGetMethods.entrySet()) {
            String path = entry.getKey();
            ExecutableElement method = entry.getValue();

            TypeMirror returnType = method.getReturnType();
            Element returnElement = controller.getTypes().asElement(returnType);
            TypeMirror entityCollectionType = getCollectionType(returnType, controller);
            if (entityCollectionType == null
                    && returnElement instanceof TypeElement) // skip primitives ( consider just type element )
            {
                if (getAnnotation(returnElement, XML_ROOT_ELEMENT) == null) {
                    /* TODO : here is only @XmlRootElement annotated elements
                     * are considered as JSON serializable ( that's true for
                     * NB generated entities ) but there could be probably
                     * other ways to serialize ( read/write REST providers )
                     * POJO classes
                     */
                    continue;
                }
                EnumMap<HttpRequests, String> paths
                        = new EnumMap<HttpRequests, String>(HttpRequests.class);
                paths.put(HttpRequests.POST, parseNoIdPath(postMethods,
                        returnType, controller));
                paths.put(HttpRequests.PUT, parseNoIdPath(putMethods,
                        returnType, controller));
                paths.put(HttpRequests.DELETE, parseNoIdPath(deleteMethods,
                        returnType, controller));
                generate((TypeElement) returnElement, path,
                        null, paths, Collections.<HttpRequests, Boolean>emptyMap(),
                        controller);
            } else {
                // collection of entities
                Element entityType = controller.getTypes().asElement(entityCollectionType);
                if (entityType instanceof TypeElement) {
                    String fqn = ((TypeElement) entityType).getQualifiedName().toString();
                    fqn2Path.put(fqn, path);
                }
            }
        }
        for (Entry<String, ExecutableElement> entry : oneParamGetMethods.entrySet()) {
            String path = entry.getKey();
            ExecutableElement method = entry.getValue();

            TypeMirror returnType = method.getReturnType();
            Element returnElement = controller.getTypes().asElement(returnType);
            if (returnElement instanceof TypeElement) {
                // TODO: return type could be a primitive type. How it should be handled ?
                if (getAnnotation(returnElement, XML_ROOT_ELEMENT) == null) {
                    /* TODO : here is only @XmlRootElement annotated elements
                     * are considered as JSON serializable ( that's true for
                     * NB generated entities ) but there could be probably
                     * other ways to serialize ( read/write REST providers )
                     * POJO classes
                     */
                    continue;
                }
                String fqn = ((TypeElement) returnElement).getQualifiedName().toString();
                String collectionPath = fqn2Path.get(fqn);
                EnumMap<HttpRequests, String> paths
                        = new EnumMap<HttpRequests, String>(HttpRequests.class);
                EnumMap<HttpRequests, Boolean> ids
                        = new EnumMap<HttpRequests, Boolean>(HttpRequests.class);
                parsePath(postMethods, returnType, paths, ids,
                        HttpRequests.POST, controller);
                parsePath(putMethods, returnType, paths, ids,
                        HttpRequests.PUT, controller);
                parsePath(deleteMethods, returnType, paths, ids,
                        HttpRequests.DELETE, controller);
                generate((TypeElement) returnElement, path,
                        collectionPath, paths, ids, controller);
            }
        }
    }

    private String parseNoIdPath(List<ExecutableElement> methods,
            TypeMirror type, CompilationController controller) {
        for (ExecutableElement method : methods) {
            List<? extends VariableElement> parameters = method.getParameters();
            boolean matches = false;
            if (parameters.size() == 0) {
                matches = true;
            } else if (parameters.size() == 1) {
                VariableElement param = parameters.get(0);
                if (controller.getTypes().isSameType(param.asType(), type)) {
                    matches = true;
                }
            } else {
                continue;
            }
            if (matches) {
                AnnotationMirror annotation = getAnnotation(method, PATH);
                if (annotation == null) {
                    return "";
                } else {
                    return getValue(annotation);
                }
            }
        }
        return null;
    }

    private void parsePath(List<ExecutableElement> methods, TypeMirror type,
            EnumMap<HttpRequests, String> paths,
            EnumMap<HttpRequests, Boolean> ids, HttpRequests request,
            CompilationController controller) {
        for (ExecutableElement method : methods) {
            List<? extends VariableElement> parameters = method.getParameters();
            boolean matches = false;
            String pathParam = null;
            if (parameters.size() == 1) {
                VariableElement param = parameters.get(0);
                if (controller.getTypes().isSameType(param.asType(), type)) {
                    matches = true;
                } else if (getAnnotation(param, PATH_PARAM) != null) {
                    pathParam = getValue(getAnnotation(param, PATH_PARAM));
                    matches = true;
                    ids.put(request, Boolean.TRUE);
                }
            } else if (parameters.size() == 2) {
                VariableElement param1 = parameters.get(0);
                VariableElement param2 = parameters.get(1);
                if (getAnnotation(param1, PATH_PARAM) != null) {
                    pathParam = getValue(getAnnotation(param1, PATH_PARAM));
                    if (controller.getTypes().isSameType(param2.asType(), type)) {
                        matches = true;
                    }
                } else if (controller.getTypes().isSameType(param1.asType(), type)) {
                    if (getAnnotation(param2, PATH_PARAM) != null) {
                        pathParam = getValue(getAnnotation(param2, PATH_PARAM));
                        matches = true;
                    }
                }
                if (matches) {
                    ids.put(request, Boolean.TRUE);
                }
            } else {
                continue;
            }
            if (matches) {
                AnnotationMirror annotation = getAnnotation(method, PATH);
                if (annotation == null) {
                    paths.put(request, "");
                } else {
                    String path = getValue(annotation);
                    if (pathParam != null) {
                        path = removeParamTemplate(path, pathParam);
                    }
                    paths.put(request, path);
                }
                break;
            }
        }
    }

    private void generate(TypeElement entity, String path,
            String collectionPath, Map<HttpRequests, String> httpPaths,
            Map<HttpRequests, Boolean> useIds,
            CompilationController controller) throws IOException {
        isModelGenerated = true;
        ModelGenerator generator = new ModelGenerator(myDescription,
                myModels, myEntities, myUi);
        generator.generateModel(entity, path,
                collectionPath, httpPaths, useIds, controller);
        generateRouter(entity, path, collectionPath, httpPaths, controller,
                generator);
    }

    private void generateRouter(TypeElement entity, String path,
            String collectionPath, Map<HttpRequests, String> httpPaths,
            CompilationController controller, ModelGenerator modelGenerator) {
        if (myModelsCount > 0) {
            myRouters.append("/*");
        }
        String name = "AppRouter";
        if (myModelsCount > 0) {
            name = name + myModelsCount;
        }
        RouterGenerator generator = new RouterGenerator(myRouters, name,
                modelGenerator);
        generator.generateRouter(entity, path, collectionPath, httpPaths, controller);
        if (myModelsCount == 0) {
            // Create HTML "view" for header identifier
            myHeader.append("<div id='");
            myHeader.append(generator.getHeaderId());
            myHeader.append("'></div>\n");
            generateContent(generator);
            if (generator.getCreateTemplate() != null) {
                myTmplCreate.append("<script type='text/template' id='");
                myTmplCreate.append(generator.getCreateTemplate());
                myTmplCreate.append("'>\n");
                myTmplCreate.append("<!--\n");
                myTmplCreate.append("\tPut your controls to create new entity here.\n\n");
                myTmplCreate.append("\tClass 'new' is used to listen on events in JS code.\n");
                myTmplCreate.append("-->\n");
                myTmplCreate.append("<button class='new'>Create</button>\n");
                myTmplCreate.append("</script>\n");
            }

            if (generator.getListItemTemplate() != null) {
                generateHeadTemplate(generator);
                myTmplList.append("<script type='text/template' id='");
                myTmplList.append(generator.getListItemTemplate());
                myTmplList.append("'>\n");
                generateItemContent(generator);
                myTmplList.append("</script>\n");
            }

            if (generator.getDetailsTemplate() != null) {
                myTmplDetails.append("<table id=\"table\"\n");
                myTmplDetails.append("   data-bind=\"ojComponent: {\n");
                myTmplDetails.append("   component: 'ojTable',\n");
                myTmplDetails.append("      data: datasource,\n");
                myTmplDetails.append("      columns: [\n");
                Set<ModelAttribute> attributes1 = modelGenerator.getAttributes();
                for (ModelAttribute attribute : attributes1) {
                    String attrName = attribute.getName();
                    String attrNameUp = attrName.toUpperCase();
                    myTmplDetails.append("            {headerText: '" + attrNameUp + "',  field: '" + attrName + "'},\n");
                }
                myTmplDetails.append("]}\">\n");
                myTmplDetails.append("</table>\n");
            }
        } else {
            myRouters.append("*/");
        }
        myModelsCount++;
    }

    private void generateHeadTemplate(RouterGenerator generator) {
        myTmplList.append("<script type='text/template' id='");
        myTmplList.append(generator.getTableHeadId());
        myTmplList.append("'>\n<thead>\n<tr>\n");

        if (generator.getModelGenerator().getIdAttribute() != null) {
            String id = generator.getModelGenerator().getIdAttribute()
                    .getName();
            myTmplList.append("<th>");
            myTmplList.append(id);
            myTmplList.append("</th>\n");
        }
        Set<ModelAttribute> attributes = generator.getModelGenerator()
                .getAttributes();
        for (ModelAttribute attribute : attributes) {
            myTmplList.append("<th>");
            myTmplList.append(attribute.getName());
            myTmplList.append("</th>\n");
        }

        myTmplList.append("</tr>\n</thead>\n</script>\n");
    }

    private void generateItemContent(RouterGenerator generator) {
        if (generator.getModelGenerator().getIdAttribute() != null) {
            String id = generator.getModelGenerator().getIdAttribute().getName();
            myTmplList.append("<td><a href='#<%= ");
            myTmplList.append(id);
            myTmplList.append(" %>'><%= ");
            myTmplList.append(id);
            myTmplList.append(" %></a></td>\n");
        }
        Set<ModelAttribute> attributes = generator.getModelGenerator().getAttributes();
        for (ModelAttribute attribute : attributes) {
            myTmplList.append("<td><%= ");
            myTmplList.append(attribute.getName());
            myTmplList.append(" %></td>\n");
        }
    }

    private void generateContent(RouterGenerator generator) {
        myContent.append("<div id='");
        myContent.append(generator.getContentId());
        myContent.append("'></div>\n");
    }

    private String removeParamTemplate(String path, String param) {
        int index = path.indexOf('{');
        String template = path;
        if (index == -1) {
            return path;
        } else {
            template = path.substring(index + 1).trim();
            int lastIndex = template.lastIndexOf('}');
            if (lastIndex == -1) {
                return path;
            }
            template = template.substring(0, lastIndex).trim();
        }
        if (!template.startsWith(param)) {
            return path;
        }
        template = template.substring(param.length()).trim();
        if (template.length() == 0 || template.charAt(0) == ':') {
            return path.substring(0, index);
        }
        return path;
    }

    private TypeMirror getCollectionType(TypeMirror type,
            CompilationController controller) {
        TypeElement collectionElement = controller.getElements().getTypeElement(
                Collection.class.getName());
        TypeMirror collectionType = controller.getTypes().erasure(
                collectionElement.asType());
        TypeMirror erasure = controller.getTypes().erasure(type);
        if (!controller.getTypes().isSubtype(erasure, collectionType)) {
            return null;
        }
        List<? extends TypeMirror> supers = controller.getTypes().directSupertypes(type);
        for (TypeMirror superType : supers) {
            erasure = controller.getTypes().erasure(superType);
            if (controller.getTypes().isSameType(erasure, collectionType)) {
                return getParameterType(superType);
            }
            TypeMirror found = getCollectionType(superType, controller);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private TypeMirror getParameterType(TypeMirror type) {
        if (type instanceof DeclaredType) {
            List<? extends TypeMirror> typeArguments = ((DeclaredType) type).getTypeArguments();
            if (typeArguments.size() == 0) {
                return null;
            }
            return typeArguments.get(0);
        }
        return null;
    }

    private String getValue(AnnotationMirror annotation) {
        if (annotation == null) {
            return null;
        }
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues
                = annotation.getElementValues();
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : elementValues.entrySet()) {
            ExecutableElement annotationMethod = entry.getKey();
            AnnotationValue value = entry.getValue();
            if (annotationMethod.getSimpleName().contentEquals("value")) {
                Object val = value.getValue();
                if (val != null) {
                    return val.toString();
                }
            }
        }
        return null;
    }

    private boolean hasJsonMedia(List<? extends AnnotationMirror> annotations) {
        AnnotationMirror consumes = getAnnotation(annotations, CONSUMES);
        AnnotationMirror produces = getAnnotation(annotations, PRODUCES);
        AnnotationMirror mimeTypeDecl = consumes == null ? produces : consumes;
        if (mimeTypeDecl == null) {
            return false;
        }
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues
                = mimeTypeDecl.getElementValues();
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : elementValues.entrySet()) {
            ExecutableElement annotationMethod = entry.getKey();
            AnnotationValue value = entry.getValue();
            if (annotationMethod.getSimpleName().contentEquals("value")) {
                Object mediaType = value.getValue();
                if (mediaType instanceof List<?>) {
                    List<?> types = (List<?>) mediaType;
                    for (Object type : types) {
                        if (type instanceof AnnotationValue) {
                            mediaType = ((AnnotationValue) type).getValue();
                            if (JSON.equals(mediaType)) {
                                return true;
                            }
                        }
                    }
                } else if (JSON.equals(mediaType)) {
                    return true;
                }
            }
        }
        return false;
    }

    static AnnotationMirror getAnnotation(List<? extends AnnotationMirror> annotations,
            String annotation) {
        for (AnnotationMirror annotationMirror : annotations) {
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            if (annotationElement instanceof TypeElement) {
                TypeElement annotationDecl = (TypeElement) annotationElement;
                if (annotationDecl.getQualifiedName().contentEquals(annotation)) {
                    return annotationMirror;
                }
            }
        }
        return null;
    }

    static AnnotationMirror getAnnotation(Element element, String annotation) {
        List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
        return getAnnotation(annotations, annotation);
    }

    private Map<String, AnnotationMirror> getAnnotions(Element element) {
        List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
        Map<String, AnnotationMirror> map = new HashMap<String, AnnotationMirror>();
        for (AnnotationMirror annotationMirror : annotations) {
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            if (annotationElement instanceof TypeElement) {
                TypeElement annotationDecl = (TypeElement) annotationElement;
                map.put(annotationDecl.getQualifiedName().toString(), annotationMirror);
            }
        }
        return map;
    }

    private RestServiceDescription myDescription;
    private JsUi myUi;
    private StringBuilder myModels;
    private StringBuilder myRouters;
    private StringBuilder myHeader;
    private StringBuilder myContent;
    private StringBuilder mySidebar;
    private StringBuilder myTmplCreate;
    private StringBuilder myTmplList;
    private StringBuilder myTmplDetails;
    private Set<String> myEntities = new HashSet<String>();
    private boolean isModelGenerated;
    private int myModelsCount;
    private boolean hasUi;

}
