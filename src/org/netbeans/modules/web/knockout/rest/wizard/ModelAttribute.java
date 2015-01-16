package org.netbeans.modules.web.knockout.rest.wizard;



/**
 * @author ads
 *
 */
class ModelAttribute {
    
    private static final String NAME = "name";
    private static final ModelAttribute PREFFERED = new ModelAttribute(NAME);

    ModelAttribute(String name){
        myName = name;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof ModelAttribute ){
            ModelAttribute attr = (ModelAttribute)obj;
            return attr.myName.equals( myName );
        }
        else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return myName.hashCode();
    }
    
    String getName(){
        return myName;
    }
    
    static ModelAttribute getPreffered(){
        return PREFFERED;
    }
    
    private String myName;

}
