package com.cloudhis.service;

import com.cloudhis.common.*;
import com.cloudhis.mapper.EncounterMapper;
import org.springframework.stereotype.Service;
import java.util.Map;
import static com.cloudhis.common.Values.*;

@Service
public class AccessService {
    private final EncounterMapper encounters;
    public AccessService(EncounterMapper encounters) { this.encounters=encounters; }
    public Map<String,Object> encounter(Map<String,Object> user,String id) {
        Map<String,Object> row=must(encounters.get(id),"就诊记录不存在");
        String role=str(user,"role_code");
        if(!role.equals("ADMIN") && !role.equals("CASHIER") && !(role.equals("DOCTOR") && str(row,"doctor_id").equals(str(user,"id")))) throw new BusinessException(403,"不能访问该患者的门诊资料");
        return row;
    }
    public void clinical(Map<String,Object> user,String id) {
        encounter(user,id);
        if(!str(user,"role_code").equals("ADMIN") && !str(user,"role_code").equals("DOCTOR")) throw new BusinessException(403,"仅接诊医生可以修改临床资料");
    }
    public void executor(Map<String,Object> user,Map<String,Object> order) {
        String role=str(user,"role_code");
        if(role.equals("ADMIN")) return;
        if(!str(order,"kind").equals(role) || !str(user,"department_id").equals(str(order,"department_id"))) throw new BusinessException(403,"不能处理其他科室的申请");
    }
}
