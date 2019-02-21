/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.ndexbio.enrichment.rest.engine.BasicEnrichmentEngineFactory;
import org.ndexbio.enrichment.rest.engine.EnrichmentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author churas
 */
public class EnrichmentHttpServletDispatcher extends HttpServletDispatcher {
    
    static Logger _logger = LoggerFactory.getLogger(EnrichmentHttpServletDispatcher.class.getSimpleName());

    private static String _version = "";
    private static String _buildNumber = "";
    private EnrichmentEngine _enrichmentEngine;
    private Thread _enrichmentEngineThread;
    
    
    public EnrichmentHttpServletDispatcher(){
        super();
        _logger.info("In constructor");
        createAndStartEnrichmentEngine();
    }
    
    protected void createAndStartEnrichmentEngine() {
        BasicEnrichmentEngineFactory fac = new BasicEnrichmentEngineFactory(Configuration.getInstance());
        
        try {
            _logger.debug("Creating Enrichment Engine from factory");
            _enrichmentEngine = fac.getEnrichmentEngine();
            _logger.debug("Starting Enrichment Engine thread");
            _enrichmentEngineThread = new Thread(_enrichmentEngine);
            _enrichmentEngineThread.start();
            _logger.debug("Enrichment Engine thread running id => " + Long.toString(_enrichmentEngineThread.getId()));
            Configuration.getInstance().setEnrichmentEngine(_enrichmentEngine);
        }
        catch(Exception ex){
            _logger.error("Unable to start enrichment engine", ex);
        }
    }

    @Override
    public void init(javax.servlet.ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        _logger.info("Entering init()");
        updateVersion();
        
        _logger.info("Exiting init()");
    }
    
    @Override
    public void destroy() {
        super.destroy();
        _logger.info("In destroy()");
        if (_enrichmentEngine != null){
            _enrichmentEngine.shutdown();
            _logger.info("Waiting for enrichment engine to shutdown");
            try {
                if (_enrichmentEngineThread != null){
                    _enrichmentEngineThread.join(10000);
                }
            }
            catch(InterruptedException ie){
                _logger.error("Caught exception waiting for enrichment engine to exit", ie);
            }
        } else {
            _logger.error("No enrichment engine found to destroy");
        
        }
    }
    
    /**
     * Reads /META-INFO/MANIFEST.MF for version and build information
     * setting _version and _buildNumber to those values if found.
     */
    private void updateVersion(){
        ServletContext application = getServletConfig().getServletContext();
        try(InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if ( inputStream !=null) {
                try {
                    Manifest manifest = new Manifest(inputStream);

                    Attributes aa = manifest.getMainAttributes();	

                    String ver = aa.getValue("NDExEnrichment-Version");
                    String bui = aa.getValue("NDExEnrichment-Build"); 
                    _logger.info("NDEx-Version: " + ver + ",Build:" + bui);
                    _buildNumber= bui.substring(0, 5);
                    _version = ver;
                } catch (IOException e) {
                    _logger.error("failed to read MANIFEST.MF", e);
                }     
            }
            else {
                _logger.error("Unable to get /META-INF/MANIFEST.MF");
            }
        } catch (IOException e1) {
            _logger.error("Failed to close InputStream from MANIFEST.MF", e1);
        }
    }
    
    public static String getVersion(){
        return _version;
    }
    
    public static String getBuildNumber(){
        return _buildNumber;
    }
}
