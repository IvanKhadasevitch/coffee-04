package controller;

import entities.CoffeeType;
import entities.enums.DisabledFlag;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import services.ICoffeeTypeService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/coffee")
public class CoffeeController {
    private static Logger log = Logger.getLogger(CoffeeController.class);

    private static final String COFFEE_MAIN = "coffee/main";
    private static final String COFFEE_CHANGE = "coffee/changeCoffee";
    private static final String COFFEE_ADD = "coffee/addCoffee";

    private ICoffeeTypeService coffeeTypeService;

    @Autowired
    public CoffeeController(ICoffeeTypeService coffeeTypeService) {
        this.coffeeTypeService = coffeeTypeService;
    }

    @RequestMapping(value="/page", method = RequestMethod.GET)
    public String mainPage(ModelMap model) {
        // del after debug
        System.out.println("->: CoffeeController.mainPage, RequestMethod.GET");

        fillModel(model);
        return COFFEE_MAIN;
    }

    @RequestMapping(value="/page", method = RequestMethod.POST)
    public String mainPage(ModelMap model, HttpServletRequest req) {
        final int DEFAULT_ID = -1;

        // del after debug
        System.out.println("->: CoffeeController.mainPage, RequestMethod.POST req: " + req);

        // check first come on the page after changeCoffeeType
        String afterChange = req.getAttribute("afterChange") != null
                ? (String) req.getAttribute("afterChange")
                : null;
        if ("on".equals(afterChange)) {
            // first come on the page after changeCoffeeType
            // coffeeTypes save in request
            fillModel(model);

            return COFFEE_MAIN;
        }

        // take coffeeTypes from Db
        List<CoffeeType> coffeeTypeList = coffeeTypeService.getAll();

        // check income parameters
        boolean changeDisabledFlag = false;
        boolean changeFlag = false;
        int coffeeTypeIdForChange = DEFAULT_ID;
        for (CoffeeType coffeeType : coffeeTypeList) {
            // determine the type of coffee for change
            String changeParameter = req.getParameter("coffeeTypeId" + String.valueOf(coffeeType.getId()));
            changeFlag = changeFlag || changeParameter != null;

            if (changeFlag && coffeeTypeIdForChange == DEFAULT_ID) {
                coffeeTypeIdForChange = coffeeType.getId();
            }

            // check disabled flag & change CoffeeType if necessary
            String disabledParameter = req.getParameter("disabled" + String.valueOf(coffeeType.getId()));
            if (disabledParameter != null && disabledParameter.equalsIgnoreCase("on")) {
                if (DisabledFlag.N.equals(coffeeType.getDisabledFlag())) {
                    // change DisabledFlag on "Y" for coffeeType in Db
                    coffeeType.setDisabledFlag(DisabledFlag.Y);
                    coffeeTypeService.update(coffeeType);
                    changeDisabledFlag = true;
                }
            }
            if (disabledParameter == null || ! "on".equalsIgnoreCase(disabledParameter)) {
                if (DisabledFlag.Y.equals(coffeeType.getDisabledFlag())) {
                    // change DisabledFlag on "N" for coffeeType in Db
                    coffeeType.setDisabledFlag(DisabledFlag.N);
                    coffeeTypeService.update(coffeeType);
                    changeDisabledFlag = true;
                }
            }
        }

        if (changeFlag) {
            // CoffeeType with id coffeeTypeIdForChange - mast be changed
            if (log.isInfoEnabled()) {
                log.info("CoffeeType with id [" + coffeeTypeIdForChange + "] - mast be changed");
            }
            CoffeeType coffeeTypeForChange = coffeeTypeService.get(coffeeTypeIdForChange);
            // coffeeTypeForChange save in session
            req.getSession().setAttribute("coffeeTypeForChange", coffeeTypeForChange);

            // refer to CoffeeType change
            return "redirect:/coffee/changeCoffee";
        } else {
            // check if DisabledFlag was changed
            if (changeDisabledFlag) {
                // take changed coffeeTypes from Db
                coffeeTypeList = coffeeTypeService.getAll();
            }
            // coffeeTypes save in request
            req.setAttribute("coffeeTypeList", coffeeTypeList);
            // show all CoffeeTypes, refer to MAIN_PAGE.
            populatePageName(model);

            return COFFEE_MAIN;
        }
    }

    @RequestMapping(value="/changeCoffee", method = {RequestMethod.GET, RequestMethod.POST})
    public String changeCoffee(ModelMap model, HttpServletRequest req) {
        final double DEFAULT_PRICE = 0;

        // del after debug
        System.out.println("->: CoffeeController.changeCoffee, req: " + req);

        // get income parameters
        String disabledParameter = req.getParameter("disabled");
        DisabledFlag disabledFlag = disabledParameter != null && "on".equalsIgnoreCase(disabledParameter)
                ? DisabledFlag.Y
                : DisabledFlag.N;

        String typeName = req.getParameter("typeName");
        typeName = typeName != null && typeName.isEmpty() ? null : typeName;
        double price = NumberUtils.toDouble(req.getParameter("price"), DEFAULT_PRICE);
        price = price < 0 ? DEFAULT_PRICE : price;

        // change coffeeTypeFromSession & save in session
        CoffeeType coffeeTypeFromSession = req.getSession()
                                              .getAttribute("coffeeTypeForChange") != null
                ? (CoffeeType) req.getSession()
                                  .getAttribute("coffeeTypeForChange")
                : null;

        if (coffeeTypeFromSession != null) {
            coffeeTypeFromSession.setDisabledFlag(disabledFlag);
            if (typeName != null) {
                coffeeTypeFromSession.setTypeName(typeName);
            }
            if (price != DEFAULT_PRICE) {
                coffeeTypeFromSession.setPrice(price);
            }
            // save coffeeTypeFromSession in session
            req.getSession().setAttribute("coffeeTypeForChange", coffeeTypeFromSession);
        }

        if ( typeName == null &&  price == DEFAULT_PRICE) {
            // first time on page, refer to COFFEE_CHANGE.
            populatePageName(model);

            return COFFEE_CHANGE;
        } else {
            if ( typeName != null && price != DEFAULT_PRICE && coffeeTypeFromSession != null) {
                // income parameters are valid, update CoffeeType in Db
                coffeeTypeService.update(coffeeTypeFromSession);

                // save in request
                req.setAttribute("afterChange", "on");

                // coffeeType updated -> refer to CoffeeType list
                return "forward:/coffee/page";

            } else {
                // income parameters are invalid
                // save errorMessage in request
                req.setAttribute("coffeeChangeErrorMsg", "Invalid parameters");
                populatePageName(model);
                // again enter parameters, refer to MAIN_PAGE.
                return COFFEE_CHANGE;
            }
        }
    }

    @RequestMapping(value="/addCoffee", method = {RequestMethod.GET, RequestMethod.POST})
    public String addCoffee(ModelMap model, HttpServletRequest req) {
        final double DEFAULT_PRICE = 0;

        // del after debug
        System.out.println("->: CoffeeController.addCoffee, req: " + req);

        // get income parameters
        String disabledParameter = req.getParameter("disabled");
        DisabledFlag disabledFlag = disabledParameter != null && "on".equalsIgnoreCase(disabledParameter)
                ? DisabledFlag.Y
                : DisabledFlag.N;

        String typeName = req.getParameter("typeName");
        typeName = typeName != null && typeName.isEmpty() ? null : typeName;
        double price = NumberUtils.toDouble(req.getParameter("price"), DEFAULT_PRICE);
        price = price < 0 ? DEFAULT_PRICE : price;

        // create CoffeeType
        CoffeeType coffeeType = new CoffeeType();
        coffeeType.setDisabledFlag(disabledFlag);
        if (typeName != null) {
            coffeeType.setTypeName(typeName);
        }
        if (price != DEFAULT_PRICE) {
            coffeeType.setPrice(price);
        }
        // save coffeeType in session
        req.getSession().setAttribute("coffeeTypeForAdd", coffeeType);

        if (typeName == null && price == DEFAULT_PRICE) {
            // first time on page, refer to COFFEE_ADD.
            populatePageName(model);

            return COFFEE_ADD;
        } else {
            if (typeName != null && price != DEFAULT_PRICE) {
                // income parameters are valid, save CoffeeType in Db
                coffeeTypeService.add(coffeeType);

                // save in request
                req.setAttribute("afterChange", "on");

                // coffeeType updated -> refer to CoffeeType list
                return "forward:/coffee/page";
            } else {
                // income parameters are invalid
                // save errorMessage in request
                req.setAttribute("coffeeChangeErrorMsg", "Invalid parameters");
                populatePageName(model);

                // again enter parameters, refer to COFFEE_ADD.
                return COFFEE_ADD;
            }
        }
    }

    private void fillModel(ModelMap model) {
        populatePageName(model);
        // save coffeeTypes in request
        model.addAttribute("coffeeTypeList", coffeeTypeService.getAll());
    }

    private void populatePageName(ModelMap model) {
        model.addAttribute("currentPageName", "coffee");
        model.addAttribute("currentTitle", "coffee.title");
    }
}
