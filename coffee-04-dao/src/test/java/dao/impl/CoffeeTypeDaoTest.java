package dao.impl;

import dao.ICoffeeTypeDao;
import entities.CoffeeType;
import entities.enums.DisabledFlag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;

@ContextConfiguration("/testContext-dao.xml")
@RunWith(SpringJUnit4ClassRunner.class)

@Transactional(transactionManager = "txManager")
public class CoffeeTypeDaoTest {
    @Autowired
    private ICoffeeTypeDao coffeeTypeDao;

    @Test
    @Rollback(value = true)
    public void getAllForDisabledFlag() {
        // make new CoffeeType
        CoffeeType coffeeType = new CoffeeType("CoffeeType TEST coffee", 12.21,
                DisabledFlag.Y);
        // take all CoffeeType from DB with DisabledFlag.Y
        List<CoffeeType> coffeeTypeList1 = coffeeTypeDao.getAllForDisabledFlag(DisabledFlag.Y);
        // take all CoffeeType from DB with DisabledFlag.N
        List<CoffeeType> coffeeTypeList3 = coffeeTypeDao.getAllForDisabledFlag(DisabledFlag.N);

        // safe CoffeeType in DB
        CoffeeType coffeeTypeSaved = coffeeTypeDao.add(coffeeType);

        // take again CoffeeType from DB with DisabledFlag.Y
        List<CoffeeType> coffeeTypeList2 = coffeeTypeDao.getAllForDisabledFlag(DisabledFlag.Y);
        assertEquals("Add not one CoffeeType with DisabledFlag.Y",
                1, coffeeTypeList2.size() - coffeeTypeList1.size());
        // take again CoffeeType from DB with DisabledFlag.N
        List<CoffeeType> coffeeTypeList4 = coffeeTypeDao.getAllForDisabledFlag(DisabledFlag.N);
        assertEquals("Add any CoffeeType with DisabledFlag.N",
                0, coffeeTypeList4.size() - coffeeTypeList3.size());

        // remove saved CoffeeType
        coffeeTypeDao.delete(coffeeTypeSaved.getId());

        // check Correctness of removal
        CoffeeType coffeeTypeGot = coffeeTypeDao.get(coffeeTypeSaved.getId());
        assertNull("CoffeeType wasn't deleted from DB", coffeeTypeGot);
    }

    @Test
    @Rollback(value = true)
    public void getAll() {
        // make new CoffeeType
        CoffeeType coffeeTypeY = new CoffeeType("CoffeeType TEST-Y coffee", 12.21,
                DisabledFlag.Y);
        CoffeeType coffeeTypeN = new CoffeeType("CoffeeType TEST-N coffee", 10.10,
                DisabledFlag.N);
        // take all CoffeeType from DB
        List<CoffeeType> coffeeTypeList1 = coffeeTypeDao.getAll();

        // safe 2 CoffeeTypes in DB
        CoffeeType coffeeTypeSavedY = coffeeTypeDao.add(coffeeTypeY);
        CoffeeType coffeeTypeSavedN = coffeeTypeDao.add(coffeeTypeN);

        // take again all CoffeeTypes from DB
        List<CoffeeType> coffeeTypeList2 = coffeeTypeDao.getAll();
        assertEquals("Add not two CoffeeTypes",
                2, coffeeTypeList2.size() - coffeeTypeList1.size());

        // remove saved CoffeeTypes
        coffeeTypeDao.delete(coffeeTypeSavedY.getId());
        coffeeTypeDao.delete(coffeeTypeSavedN.getId());

        // check Correctness of removal
        CoffeeType coffeeTypeGot = coffeeTypeDao.get(coffeeTypeSavedY.getId());
        assertNull("CoffeeType TEST-Y wasn't deleted from DB", coffeeTypeGot);

        coffeeTypeGot = coffeeTypeDao.get(coffeeTypeSavedN.getId());
        assertNull("CoffeeType TEST-N wasn't deleted from DB", coffeeTypeGot);
    }
}
