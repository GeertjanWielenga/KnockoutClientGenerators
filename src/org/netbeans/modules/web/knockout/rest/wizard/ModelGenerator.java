package org.netbeans.modules.web.knockout.rest.wizard;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.web.knockout.rest.wizard.JSClientGenerator.HttpRequests;
import org.netbeans.modules.web.knockout.rest.wizard.JSClientGenerator.MethodType;
import org.netbeans.modules.web.knockout.rest.wizard.RestPanel.JsUi;
import org.netbeans.modules.websvc.rest.model.api.RestServiceDescription;
import org.netbeans.modules.websvc.rest.spi.MiscUtilities;
import org.netbeans.modules.websvc.rest.spi.RestSupport;

/**
 * @author ads
 *
 */
class ModelGenerator {

    private static final String SLASH = "/";                        // NOI18N
    private static final String ID = "javax.persistence.Id";         // NOI18N

    ModelGenerator(RestServiceDescription description,
            StringBuilder builder, Set<String> entities, JsUi ui) {
        myDescription = description;
        myCommonModels = builder;
        myEntities = entities;
        myUi = ui;
    }

    void generateModel(TypeElement entity, String path,
            String collectionPath, Map<HttpRequests, String> httpPaths,
            Map<HttpRequests, Boolean> useIds,
            CompilationController controller) throws IOException {
        String fqn = entity.getQualifiedName().toString();
        String name = entity.getSimpleName().toString();
        myModelName = suggestModelName(name);

        String url = getUrl(path);
        String entitiesName = myModelName.toLowerCase() + "s";
        myCommonModels.append("self.items");                            // NOI18N
        myCommonModels.append(" = ko.observableArray();");       // NOI18N
        myCommonModels.append("\n");                                   // NOI18N
        myCommonModels.append("$.getJSON(\"").append(url).append("\").\nthen(function(").append(entitiesName).append(") {");
        myCommonModels.append("\n  $.each(").append(entitiesName).append(", function() {");                                   // NOI18N
        myCommonModels.append("\n    self.items.push({");                                   // NOI18N

        myAttributes = new HashSet<ModelAttribute>();
        String parsedData = parse(entity, controller);
        if (parsedData != null) {
            myCommonModels.append(parsedData);
        }
        if (!myAttributes.isEmpty()) {
            // suggest what attribute could be used as displayName 

            ModelAttribute preffered = ModelAttribute.getPreffered();
            if (myAttributes.contains(preffered)) {
                myDisplayNameAlias = preffered.getName();
            } else if (myIdAttribute == null) {
                myDisplayNameAlias = myAttributes.iterator().next().getName();
            } else {
                myDisplayNameAlias = myIdAttribute.getName();
            }
        } else if (myIdAttribute != null) {
            myDisplayNameAlias = myIdAttribute.getName();
        }

        if (collectionPath == null) {
            return;
        }
    }

    JsUi getUi() {
        return myUi;
    }

    boolean hasCollection() {
        return myCollectionModelName != null;
    }

    Set<ModelAttribute> getAttributes() {
        return myAttributes;
    }

    String getDisplayNameAlias() {
        return myDisplayNameAlias;
    }

    String getModelName() {
        return myModelName;
    }

    String getCollectionModelName() {
        return myCollectionModelName;
    }

    ModelAttribute getIdAttribute() {
        return myIdAttribute;
    }

    private String getUrl(String relativePath) throws IOException {
        Project project = FileOwnerQuery.getOwner(myDescription.getFile());
        RestSupport restSupport = project.getLookup().lookup(RestSupport.class);
        String applicationPath = restSupport.getApplicationPath();
        String uri = myDescription.getUriTemplate();

        if (applicationPath == null) {
            applicationPath = uri;
        } else {
            applicationPath = addUrlPath(applicationPath, uri);
        }
        applicationPath = addUrlPath(applicationPath, relativePath);

        return addUrlPath(MiscUtilities.getContextRootURL(project), applicationPath);
    }

    private String suggestModelName(String name) {
        if (myEntities.contains(name)) {
            String newName;
            int index = 1;
            while (true) {
                newName = name + index;
                if (!myEntities.contains(newName)) {
                    myEntities.add(newName);
                    return newName;
                }
                index++;
            }
        } else {
            myEntities.add(name);
        }
        return name;
    }

    private String parse(TypeElement entity, CompilationController controller) {
        /*
         *  parse entity and generate attributes:
         *  1) idAttribute
         *  2) primitive attributes if any
         *  3) do not include attributes with complex type  
         */
        Set<String> attributes = parseBeanMethods(entity, controller);

        List<VariableElement> fields = ElementFilter.fieldsIn(
                controller.getElements().getAllMembers(entity));
        VariableElement id = null;
        for (VariableElement field : fields) {
            if (JSClientGenerator.getAnnotation(field, ID) != null) {
                boolean has = attributes.remove(field.getSimpleName().toString());
                if (has) {
                    id = field;
                    break;
                }
            }
        }
        StringBuilder builder = new StringBuilder();
//        if (id != null) {
//            String idAttr = id.getSimpleName().toString();
////            builder.append("\nidAttribute : '");                        // NOI18N
////            builder.append(idAttr);
//            builder.append("'");                                        // NOI18N
//            if (attributes.size() > 0) {
//                builder.append(',');
//            }
//            myIdAttribute = new ModelAttribute(idAttr);
//        }

        if (attributes.size() > 0) {
            builder.append("\n");
            for (String attribute : attributes) {
                myAttributes.add(new ModelAttribute(attribute));
                builder.append(attribute).append(": ko.observable(this.").append(attribute).append("),");                                   // NOI18N
                builder.append("\n");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("\n});");                                   // NOI18N
            builder.append("\n  });");                                   // NOI18N
            builder.append("\n    });");                                     // NOI18N
        }

        if (builder.length() > 0) {
            return builder.toString();
        } else {
            return null;
        }
    }

    private Set<String> parseBeanMethods(TypeElement entity,
            CompilationController controller) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(
                controller.getElements().getAllMembers(entity));
        Set<String> result = new HashSet<String>();
        Map<String, TypeMirror> getAttrs = new HashMap<String, TypeMirror>();
        Map<String, TypeMirror> setAttrs = new HashMap<String, TypeMirror>();
        for (ExecutableElement method : methods) {
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }

            Object[] attribute = getAttrName(method, controller);
            if (attribute == null) {
                continue;
            }
            String name = (String) attribute[1];
            TypeMirror type = (TypeMirror) attribute[2];
            if (attribute[0] == MethodType.GET) {
                if (findAccessor(name, type, getAttrs, setAttrs, controller)) {
                    result.add(name);
                }
            } else {
                if (findAccessor(name, type, setAttrs, getAttrs, controller)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    private boolean findAccessor(String name, TypeMirror type,
            Map<String, TypeMirror> map1, Map<String, TypeMirror> map2,
            CompilationController controller) {
        TypeMirror typeMirror = map2.remove(name);
        if (typeMirror != null
                && controller.getTypes().isSameType(typeMirror, type)) {
            return true;
        } else {
            map1.put(name, type);
        }
        return false;
    }

    private Object[] getAttrName(ExecutableElement method,
            CompilationController controller) {
        String name = method.getSimpleName().toString();
        if (name.startsWith("set")) {                               // NOI18N
            TypeMirror returnType = method.getReturnType();
            if (returnType.getKind() != TypeKind.VOID) {
                return null;
            }
            List<? extends VariableElement> parameters = method.getParameters();
            if (parameters.size() != 1) {
                return null;
            }
            VariableElement param = parameters.get(0);
            TypeMirror type = param.asType();
            if (isSimple(type, controller)) {
                return new Object[]{MethodType.SET, lowerFirstLetter(
                    name.substring(3)), type};
            } else {
                return null;
            }
        }
        int start = 0;
        if (name.startsWith("get")) {                                   // NOI18N
            start = 3;
        } else if (name.startsWith("is")) {                              // NOI18N
            start = 2;
        }
        if (start > 0) {
            List<? extends VariableElement> parameters = method.getParameters();
            if (parameters.size() != 0) {
                return null;
            }
            TypeMirror returnType = method.getReturnType();
            if (isSimple(returnType, controller)) {
                return new Object[]{MethodType.GET, lowerFirstLetter(
                    name.substring(start)), returnType};
            } else {
                return null;
            }
        }
        return null;
    }

    private String lowerFirstLetter(String name) {
        if (name.length() <= 1) {
            return name;
        }
        char firstLetter = name.charAt(0);
        if (Character.isUpperCase(firstLetter)) {
            return Character.toLowerCase(firstLetter) + name.substring(1);
        }
        return name;
    }

    /*
     * returns true if type is primitive or String
     */
    private boolean isSimple(TypeMirror typeMirror, CompilationController controller) {
        if (typeMirror.getKind().isPrimitive()) {
            return true;
        }
        Element fieldTypeElement = controller.getTypes().asElement(typeMirror);
        TypeElement stringElement = controller.getElements().
                getTypeElement(String.class.getName());
        if (stringElement != null && stringElement.equals(fieldTypeElement)) {
            return true;
        }

        if (fieldTypeElement != null) {
            PackageElement pack = controller.getElements().getPackageOf(
                    fieldTypeElement);
            if (pack.getQualifiedName().contentEquals("java.lang")) {      // NOI18N
                try {
                    if (controller.getTypes().unboxedType(typeMirror) != null) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    // just skip field
                }
            }
        }

        return false;
    }

    private void overrideMethod(String url, String path, Boolean useId,
            HttpRequests request, StringBuilder builder) throws IOException {
        if (path == null) {
            builder.append("if(method=='");                              // NOI18N
            builder.append(request.toString());
            builder.append("'){\n");                                     // NOI18N
            builder.append("return false;\n}\n");                        // NOI18N
        } else {
            path = getUrl(path);
            StringBuilder newUrlSnippet = new StringBuilder();
            boolean isModified = false;
            if (!url.equals(path)) {
                newUrlSnippet.append("options.url = '");                 // NOI18N
                newUrlSnippet.append(path);
                isModified = true;
            }
            if (useId != null && useId) {
                if (isModified) {
                    if (!path.endsWith("/")) {
                        newUrlSnippet.append('/');
                    }
                    newUrlSnippet.append("'+model.id;\n");
                }
            } else {
                if (isModified) {
                    newUrlSnippet.append("';\n");
                } else {
                    newUrlSnippet.append("options.url = '");             // NOI18N
                    newUrlSnippet.append(path);
                    newUrlSnippet.append("';\n");
                }
            }
            if (newUrlSnippet.length() == 0) {
                return;
            }
            builder.append("if(method=='");                         // NOI18N
            builder.append(request.toString());
            builder.append("'){\n");                                // NOI18N
            builder.append(newUrlSnippet);
            builder.append("}\n");                                  // NOI18N
        }
    }

    private String addUrlPath(String path, String uri) {
        if (uri.startsWith(SLASH)) {
            if (path.endsWith(SLASH)) {
                path = path + uri.substring(1);
            } else {
                path = path + uri;
            }
        } else {
            if (path.endsWith(SLASH)) {
                path = path + uri;
            } else {
                path = path + SLASH + uri;
            }
        }
        return path;
    }

    private StringBuilder myCommonModels;
    private RestServiceDescription myDescription;
    private Set<String> myEntities;
    private JsUi myUi;
    private Set<ModelAttribute> myAttributes;
    private String myDisplayNameAlias;
    private String myModelName;
    private String myCollectionModelName;
    private ModelAttribute myIdAttribute;
}
