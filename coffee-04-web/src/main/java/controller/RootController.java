package controller;

import entities.CoffeeOrderItem;
import entities.CoffeeType;
import entities.enums.DisabledFlag;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import services.ICoffeeOrderService;
import services.ICoffeeTypeService;
import services.IConfigurationService;
import vo.CoffeeOrderAndCost;
import vo.CoffeeOrderItemVo;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

@Controller
@RequestMapping("/")
public class RootController {
    private static Logger log = Logger.getLogger(RootController.class);
    private static final String MAIN = "orders/main";
    private static final String DELIVERY_ORDER = "orders/deliveryOrder";
    private static final String SHOW_ORDER = "orders/showOrder";

    private ICoffeeTypeService coffeeTypeService;
    private IConfigurationService configurationService;
    private ICoffeeOrderService coffeeOrderService;

    @Autowired
    public RootController(ICoffeeTypeService coffeeTypeService, IConfigurationService configurationService,
                          ICoffeeOrderService coffeeOrderService) {
        this.coffeeTypeService = coffeeTypeService;
        this.configurationService = configurationService;
        this.coffeeOrderService = coffeeOrderService;
    }

    @RequestMapping(value={"/homePage", ""}, method = {RequestMethod.GET, RequestMethod.POST})
    public String mainPage(ModelMap model, HttpServletRequest req) {
        // del after debug
        System.out.println("->: RootController.mainPage req: " + req);

        // get coffeeType list from Db where DisabledFlag.N for UI
        List<CoffeeType> coffeeTypeList = coffeeTypeService.getAllForDisabledFlag(DisabledFlag.N);

        fillModel(model, req, coffeeTypeList);

        // validate income parameters from form
        List<CoffeeOrderItemVo> coffeeOrderItemVoList = new LinkedList<>();
        for (CoffeeType coffeeType : coffeeTypeList) {
            String parameterName = "orderQuantity" + String.valueOf(coffeeType.getId());
            int orderQuantity = NumberUtils.toInt(req.getParameter(parameterName), 0);
            orderQuantity = orderQuantity < 0 ? 0 : orderQuantity;
            // save not null order item in CoffeeOrderItemVo
            if (orderQuantity != 0) {
                CoffeeOrderItemVo coffeeOrderItemVo = new CoffeeOrderItemVo();
                coffeeOrderItemVo.setCoffeeTypeId(coffeeType.getId());
                coffeeOrderItemVo.setCoffeeTypeName(coffeeType.getTypeName());
                coffeeOrderItemVo.setCoffeePrice(coffeeType.getPrice());
                coffeeOrderItemVo.setQuantity(orderQuantity);
                coffeeOrderItemVoList.add(coffeeOrderItemVo);
            }
        }

        // check if any order item exist
        if (coffeeOrderItemVoList.isEmpty()) {
            // no Coffee Type was chosen -> refer again choose Coffee Type
            if (log.isInfoEnabled()) {
                log.info("No Coffee Type was chosen. Refer again choose Coffee Type");
            }
            return MAIN;
        } else {
            // chosen valid Coffee Types -> refer forward to make Orders
            if (log.isInfoEnabled()) {
                log.info("Chosen valid Coffee Types. Refer to make Orders for Coffee Types: " + coffeeOrderItemVoList);
            }
            // save coffeeOrderItemVoList in session
            req.getSession().setAttribute("coffeeOrderItemVoList", coffeeOrderItemVoList);

            return "redirect:/deliveryOrder";
        }
    }

    @RequestMapping(value = "/deliveryOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String deliveryOrder(ModelMap model, HttpServletRequest req) {
        final String DEFAULT_STRING = "NOT ASSIGNED";

        // del after debug
        System.out.println("->: RootController.deliveryOrder req: " + req);

        // validate income parameters from form
        String customerName = req.getParameter("customerName");
        customerName = customerName != null && customerName.isEmpty() ? null : customerName;

        String deliveryAddress = req.getParameter("deliveryAddress");
        deliveryAddress = deliveryAddress != null && deliveryAddress.isEmpty() ? null : deliveryAddress;
        List<CoffeeOrderItemVo> coffeeOrderItemVoList = req.getSession()
                                                           .getAttribute("coffeeOrderItemVoList") != null
                ? (List<CoffeeOrderItemVo>) req.getSession()
                                               .getAttribute("coffeeOrderItemVoList")
                : null;

        // del after debug
        System.out.println("coffeeOrderItemVoList: " + coffeeOrderItemVoList);

        if (deliveryAddress == null) {
            // not exist any delivery Address - try again input
            if (log.isInfoEnabled()) {
                log.info("Not exist any delivery Address - try again input");
            }
            fillModel(model, customerName, "Enter delivery address, please.");
            return DELIVERY_ORDER;
        } else {
            // create order & save in DB
            customerName = customerName == null ? DEFAULT_STRING : customerName;
            CoffeeOrderAndCost coffeeOrderAndCost = coffeeOrderService.makeOrder(customerName,
                    deliveryAddress, transformer(coffeeOrderItemVoList));
            if (coffeeOrderAndCost == null) {
                if (log.isInfoEnabled()) {
                    log.info("No any Order items exist. Choose coffee, please.");
                }
                // no Order items exist
                // save error message in request
                req.setAttribute("orderErrorMsg", "Choose coffee, please.");

                // empty order items list - refer to choose coffee
                return "forward:/homePage";
            } else {
                if (log.isInfoEnabled()) {
                    log.info("There is Order: " + coffeeOrderAndCost);
                }
                // save coffeeOrderAndCost in session
                req.getSession().setAttribute("coffeeOrderAndCost", coffeeOrderAndCost);

                // order successfully create - show order
                return "redirect:/showOrder";
            }
        }
    }
    @RequestMapping(value = "/showOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String deliveryOrder(ModelMap model) {
        populatePageName(model);
        return SHOW_ORDER;
    }
    private List<CoffeeOrderItem> transformer(List<CoffeeOrderItemVo> coffeeOrderItemVoList) {
        List<CoffeeOrderItem> coffeeOrderItemList = new LinkedList<>();
        if (coffeeOrderItemVoList != null) {
            for (CoffeeOrderItemVo itemVo : coffeeOrderItemVoList) {
                CoffeeOrderItem coffeeOrderItem = new CoffeeOrderItem();
                coffeeOrderItem.setType(coffeeTypeService.get(itemVo.getCoffeeTypeId()));
                coffeeOrderItem.setQuantity(itemVo.getQuantity());
                coffeeOrderItemList.add(coffeeOrderItem);
            }
        }
        return coffeeOrderItemList;
    }

    private void fillModel(ModelMap model, HttpServletRequest req, List<CoffeeType> coffeeTypeList) {
        populatePageName(model);

        // get number of freeCup from Db
        String freeCup = configurationService.getValue("n");
        if (freeCup != null) {
            // save freeCup in session
            req.getSession().setAttribute("freeCup", freeCup);
        }
        // coffeeType list save in request
        model.addAttribute("coffeeTypeList", coffeeTypeList);
    }

    private void fillModel(ModelMap model, String customerName, String deliveryErrorMsg) {
        populatePageName(model);
        // save customerName in request
        model.addAttribute("customerName", customerName);
        // save error message in request
        model.addAttribute("deliveryErrorMsg", deliveryErrorMsg);
    }

    private void populatePageName(ModelMap model) {
        model.addAttribute("currentPageName", "orders");
        model.addAttribute("currentTitle", "orders.title");
    }
}
