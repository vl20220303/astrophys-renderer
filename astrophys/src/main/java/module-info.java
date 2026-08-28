module astrophys.renderer {
    requires java.base;
    requires java.desktop;
    requires jdk.incubator.vector;

    exports utils.renderer.astrophys.camera;
    exports utils.renderer.astrophys.generator;
    exports utils.renderer.astrophys.runners;
    exports utils.renderer.astrophys.settings;
    exports utils.renderer.astrophys.userinterface;
    exports utils.renderer.astrophys.utils;
}