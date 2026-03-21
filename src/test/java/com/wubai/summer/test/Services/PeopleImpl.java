package com.wubai.summer.test.Services;

import com.wubai.summer.annotation.Component;


@Component
public class PeopleImpl implements People {
    @Override
    public String addPeople(String name) {
        return name + " has be added !" ;
    }

    @Override
    public String deletePeople(Integer id) {
        return "the people of  = " + id  + "  = has be deleted !" ;
    }
}
