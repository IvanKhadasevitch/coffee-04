package controller;

import entities.Configuration;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import services.IConfigurationService;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController {
    private static Logger log = Logger.getLogger(ConfigurationController.class);

    private static final String CONFIGURATION_MAIN = "configuration/main";

    private IConfigurationService configurationService;

    @Autowired
    public ConfigurationController(IConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @RequestMapping(value="/page", method = {RequestMethod.GET, RequestMethod.POST})
    public String mainPage(ModelMap model, HttpServletRequest req) {
        final int DEFAULT_FREE_CUP = -1;
        final double DEFAULT_DELIVERY = -1;

        // del after debug
        System.out.println("->: ConfigurationController.mainPage,  req: " + req);

        // check freeCup
        int freeCup = NumberUtils.toInt(req.getParameter("freeCup"), DEFAULT_FREE_CUP);
        if (freeCup < 1) {
            // use value from Db or default free cup value
            freeCup = Integer.valueOf(configurationService.getValue("n"));

        } else {
            // check exist "n" configuration in Db
            configurationSaveOrUpdate(String.valueOf(freeCup), "n");
        }
        // save freeCup in session
        req.getSession().setAttribute("freeCup", freeCup);

        // check freeDelivery
        double freeDelivery = NumberUtils.toDouble(req.getParameter("freeDelivery"), DEFAULT_DELIVERY);
        if (freeDelivery < 0) {
            // use value from Db or default value
            freeDelivery = Double.valueOf(configurationService.getValue("x"));

        } else {
            // check exist "x" configuration in Db
            configurationSaveOrUpdate(String.valueOf(freeDelivery), "x");
        }
        // save freeDelivery in session
        req.getSession().setAttribute("freeDelivery", freeDelivery);

        // check deliveryPrice
        double deliveryPrice = NumberUtils.toDouble(req.getParameter("deliveryPrice"), DEFAULT_DELIVERY);
        if (deliveryPrice < 0) {
            // use value from Db or default value
            deliveryPrice = Double.valueOf(configurationService.getValue("m"));

        } else {
            // check exist "m" configuration in Db
            configurationSaveOrUpdate(String.valueOf(deliveryPrice), "m");
        }
        // save deliveryPrice in session
        req.getSession().setAttribute("deliveryPrice", deliveryPrice);
        populatePageName(model);

        // refer to CONFIGURATION_MAIN
        return CONFIGURATION_MAIN;
    }
    private void configurationSaveOrUpdate(String configurationValue, String configurationKey) {
        // check exist configuration in Db
        Configuration configuration = configurationService.get(configurationKey);
        if (configuration == null) {
            // save configuration in DB
            configuration = new Configuration();
            configuration.setId(configurationKey);
            configuration.setValue(configurationValue);
            configuration = configurationService.add(configuration);
        } else {
            // update configuration in Db
            configuration.setValue(configurationValue);
            configurationService.update(configuration);
        }
    }

    private void populatePageName(ModelMap model) {
        model.addAttribute("currentPageName", "configuration");
        model.addAttribute("currentTitle", "configuration.title");
    }
}
