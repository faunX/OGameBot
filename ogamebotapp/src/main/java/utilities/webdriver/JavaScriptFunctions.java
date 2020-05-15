package utilities.webdriver;

/**
 * Created by jarndt on 5/5/17.
 */
public class JavaScriptFunctions {
    public static final String XPATH_FUNCTION =
            "var $x = function(xpathToExecute){\n" +
            "  var result = [];\n" +
            "  var nodesSnapshot = document.evaluate(xpathToExecute, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null );\n" +
            "  for ( var i=0 ; i < nodesSnapshot.snapshotLength; i++ ){\n" +
            "    result.push( nodesSnapshot.snapshotItem(i) );\n" +
            "  }\n" +
            "  return result;\n" +
            "};";


    public static Object fillFormByXpath(DriverController driverController, String xpath, String value){
        return fillFormByXpath(driverController,xpath,value,0);
    }public static Object fillFormByXpath(DriverController driverController, String xpath, String value, int indexNum){
        return driverController.executeJavaScript(XPATH_FUNCTION+
                "$x(\""+xpath+"\")["+indexNum+"].value=\""+value+"\";"
        );
    }
}
