import java.util.Iterator;
import java.util.Map;

import soot.*;
import soot.jimple.*;
import soot.options.Options;


public class AndroidInstrument {

    public static void main(String[] args) {

        //prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk);

        Options.v().set_allow_phantom_refs(true);

        //output as APK, too//-f J
//        Options.v().set_output_format(Options.output_format_dex);

        Options.v().set_soot_classpath(System.getProperty("java.class.path"));
        Options.v().set_prepend_classpath(true);

        // resolve the PrintStream and System soot-classes
        Scene.v().addSootBasicClasses();
        Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);

        /*
        SootClass sClass = new SootClass("out", Modifier.PUBLIC);
        sClass.setSuperclass(Scene.v().getSootClass("java.io.PrintStream"));
        Scene.v().addClass(sClass);
        */



        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

            @Override
            protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
                final PatchingChain<Unit> units = b.getUnits();

                //important to use snapshotIterator here
                for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
                    final Unit u = iter.next();
                    u.apply(new AbstractStmtSwitch() {

                        public void caseAssignStmt(AssignStmt stmt) {
                            if (stmt.containsInvokeExpr()) {
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();

                                if (invokeExpr.getMethod().getName().contains("findViewById")) {
                                    Local tmpRef = addTmpRef(b);
                                    Local tmpRef2 = addTmpRef(b);
                                    Local tmpString = addTmpString(b);

                                    Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);

                                    // insert "tmpRef = java.lang.System.out;"
                                    units.insertBefore(Jimple.v().newAssignStmt(
                                            tmpRef, Jimple.v().newStaticFieldRef(
                                                    Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

                                    units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                                            StringConstant.v("[NUDEBUG] findViewById called with ID: ")), u);

                                    // insert "tmpRef.println(tmpString);"
                                    SootMethod print = Scene.v().getSootClass("java.io.PrintStream").getMethod("void print(java.lang.String)");
                                    SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(int)");

                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, print.makeRef(), tmpString)), u);

                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), invokeExpr.getArg(0))), u);

                                    //check that we did not mess up the Jimple
                                    b.validate();
                                }
                            }
                        }


                        public void caseInvokeStmt(InvokeStmt stmt) {
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            System.out.println(stmt.toString());
                            if(invokeExpr.getMethod().getName().contains("setContentView")) {
                                if (invokeExpr.getArg(0).getType().toString().equals("int")) {

                                    Local tmpRef = addTmpRef(b);
                                    Local tmpRef2 = addTmpRef(b);
                                    Local tmpString = addTmpString(b);

                                    Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

                                    // insert "tmpRef = java.lang.System.out;"
                                    units.insertBefore(Jimple.v().newAssignStmt(
                                            tmpRef, Jimple.v().newStaticFieldRef(
                                                    Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

                                    // insert "tmpLong = 'HELLO';"
                                    units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                                            StringConstant.v("[NUDEBUG] setContentView called with ID: ")), u);

                                    // insert "tmpRef.println(tmpString);"
                                    //SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.Integer)");
                                    SootMethod print = Scene.v().getSootClass("java.io.PrintStream").getMethod("void print(java.lang.String)");

                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, print.makeRef(), tmpString)), u);

                                    SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(int)");
                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), invokeExpr.getArg(0))), u);

                                    //check that we did not mess up the Jimple
                                    b.validate();
                                } else if (invokeExpr.getArg(0).getType().toString().contains("android.view.View")) {
                                    // TODO
                                    /*
                                    Local tmpRef = addTmpRef(b);
                                    Local tmpRef2 = addTmpRef(b);
                                    Local tmpInt = addTmpInt(b);
                                    Local tmpString = addTmpString(b);
                                    Local localVal = null;

                                    Value argVal = invokeExpr.getArg(0);
                                    if (!(argVal instanceof Local)) {
                                         localVal = (Local)argVal;
                                    }


                                    Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
                                    Scene.v().addBasicClass("android.view.View", SootClass.SIGNATURES);

                                    // insert "tmpRef = java.lang.System.out;"
                                    units.insertBefore(Jimple.v().newAssignStmt(
                                            tmpRef, Jimple.v().newStaticFieldRef(
                                                    Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

                                    // insert "tmpLong = 'HELLO';"
                                    units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                                            StringConstant.v("[NUDEBUG] setContentView called with ID: ")), u);

                                    System.out.println("ANDROID VIEW METHODS COUNT: " + Scene.v().getSootClass("Android.view.View").getMethodCount());
                                    for (SootMethod m : Scene.v().getSootClass("Android.view.View").getMethods()) {
                                        System.out.println("************" + m.toString());
                                    }


                                    //SootMethod getId = Scene.v().getSootClass("Android.view.View").getMethod("int getId()");

                                    //units.insertBefore(Jimple.v().newAssignStmt(tmpInt,
                                    //        Jimple.v().newVirtualInvokeExpr(localVal, getId.makeRef())), u);


                                    // insert "tmpRef.println(tmpString);"
                                    //SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.Integer)");
                                    SootMethod print = Scene.v().getSootClass("java.io.PrintStream").getMethod("void print(java.lang.String)");

                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, print.makeRef(), tmpString)), u);

                                    SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(int)");
                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), invokeExpr.getArg(0))), u);

                                    //check that we did not mess up the Jimple
                                    b.validate();
                                    */
                                }
                            } else if (invokeExpr.toString().contains("java.net.URL") && invokeExpr.getMethod().toString().contains("init")) {
                                System.out.println("[NUDEBUG] " + invokeExpr.toString());
                                SootMethod m = invokeExpr.getMethod();
                                Value protocol = null;
                                Value v = null;

                                if (m.getParameterCount() == 1) { // URL(String spec)
                                    v = invokeExpr.getArg(0);
                                } else if (m.getParameterCount() == 2) { //URL(URL context, String spec)
                                    v = invokeExpr.getArg(1);
                                } else if (m.getParameterCount() == 3) {
                                    // URL(String protocol, String host, String file),  or
                                    // URL(URL context, String spec, URLStreamHandler handler)
                                    v = invokeExpr.getArg(1);
                                } else if (m.getParameterCount() == 4) {
                                    // URL(String protocol, String host, int port, String file)
                                    v = invokeExpr.getArg(1);
                                } else if (m.getParameterCount() == 5) {
                                    // URL(String protocol, String host, int port, String file, URLStreamHandler handler)
                                    v = invokeExpr.getArg(1);
                                }

                                Local tmpRef = addTmpRef(b);
                                Local tmpRef2 = addTmpRef(b);
                                Local tmpString = addTmpString(b);

                                Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

                                // insert "tmpRef = java.lang.System.out;"
                                units.insertBefore(Jimple.v().newAssignStmt(
                                        tmpRef, Jimple.v().newStaticFieldRef(
                                                Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

                                // insert "tmpLong = 'HELLO';"
                                units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                                        StringConstant.v("[NUDEBUG] Found URL (java.net.URL) : ")), u);

                                // insert "tmpRef.println(tmpString);"
                                SootMethod print = Scene.v().getSootClass("java.io.PrintStream").getMethod("void print(java.lang.String)");

                                units.insertBefore(Jimple.v().newInvokeStmt(
                                        Jimple.v().newVirtualInvokeExpr(tmpRef, print.makeRef(), tmpString)), u);

                                SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
                                units.insertBefore(Jimple.v().newInvokeStmt(
                                        Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), v)), u);

                                b.validate();

                            } else if (invokeExpr.toString().contains("org.apache.http.client.methods.HttpPost") && invokeExpr.getMethod().toString().contains("init")) {
                                if (invokeExpr.getMethod().getParameterType(0).getEscapedName().equals("java.lang.String")) {
                                    Value v = invokeExpr.getArg(0);

                                    Local tmpRef = addTmpRef(b);
                                    Local tmpRef2 = addTmpRef(b);
                                    Local tmpString = addTmpString(b);

                                    Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

                                    // insert "tmpRef = java.lang.System.out;"
                                    units.insertBefore(Jimple.v().newAssignStmt(
                                            tmpRef, Jimple.v().newStaticFieldRef(
                                                    Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

                                    // insert "tmpLong = 'HELLO';"
                                    units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                                            StringConstant.v("[NUDEBUG] Found URL for HTTP POST : ")), u);

                                    // insert "tmpRef.println(tmpString);"
                                    SootMethod print = Scene.v().getSootClass("java.io.PrintStream").getMethod("void print(java.lang.String)");

                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, print.makeRef(), tmpString)), u);

                                    SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), v)), u);

                                    b.validate();
                                }
                            } else if (invokeExpr.toString().contains("org.apache.http.client.methods.HttpGet") && invokeExpr.getMethod().toString().contains("init")) {
                                if (invokeExpr.getMethod().getParameterType(0).getEscapedName().equals("java.lang.String")) {
                                    Value v = invokeExpr.getArg(0);

                                    Local tmpRef = addTmpRef(b);
                                    Local tmpRef2 = addTmpRef(b);
                                    Local tmpString = addTmpString(b);

                                    Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

                                    // insert "tmpRef = java.lang.System.out;"
                                    units.insertBefore(Jimple.v().newAssignStmt(
                                            tmpRef, Jimple.v().newStaticFieldRef(
                                                    Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

                                    // insert "tmpLong = 'HELLO';"
                                    units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                                            StringConstant.v("[NUDEBUG] Found URL for HTTP GET: ")), u);

                                    // insert "tmpRef.println(tmpString);"
                                    SootMethod print = Scene.v().getSootClass("java.io.PrintStream").getMethod("void print(java.lang.String)");

                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, print.makeRef(), tmpString)), u);

                                    SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
                                    units.insertBefore(Jimple.v().newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), v)), u);

                                    b.validate();
                                }
                            }
                        }
                    });
                }
            }


        }));
        soot.Main.main(args);
    }

    private static Local addTmpRef(Body body)
    {
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        body.getLocals().add(tmpRef);
        return tmpRef;
    }

    private static Local addTmpString(Body body)
    {
        Local tmpString = Jimple.v().newLocal("tmpString", RefType.v("java.lang.String"));
        body.getLocals().add(tmpString);
        return tmpString;
    }

    private static Local addTmpInt(Body body)
    {
        Local tmpInt = Jimple.v().newLocal("tmpInt", RefType.v("int"));
        body.getLocals().add(tmpInt);
        return tmpInt;
    }
}

