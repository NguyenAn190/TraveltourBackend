package com.main.traveltour.restcontroller.superadmin;

import com.main.traveltour.dto.UsersDto;
import com.main.traveltour.dto.superadmin.AccountDto;
import com.main.traveltour.dto.superadmin.DataAccountDto;
import com.main.traveltour.entity.*;
import com.main.traveltour.service.RolesService;
import com.main.traveltour.service.UsersService;
import com.main.traveltour.service.agent.AgenciesService;
import com.main.traveltour.service.agent.HotelsService;
import com.main.traveltour.service.agent.TransportationBrandsService;
import com.main.traveltour.service.agent.VisitLocationsService;
import com.main.traveltour.service.utils.EmailService;
import com.main.traveltour.utils.EntityDtoUtils;
import com.main.traveltour.utils.GenerateNextID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1")
public class AccountAPI {

    @Autowired
    private UsersService usersService;

    @Autowired
    private RolesService rolesService;

    @Autowired
    private AgenciesService agenciesService;

    @Autowired
    private HotelsService hotelsService;

    @Autowired
    private TransportationBrandsService transportationBrandsService;

    @Autowired
    private VisitLocationsService visitLocationsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @GetMapping("superadmin/account/find-all-account-role-is-guild")
    private ResponseObject findUsersByRolesIsGuild() {
        List<Users> items = usersService.findUsersByRolesIsGuild();
        if (items.isEmpty()) {
            return new ResponseObject("404", "Không tìm thấy dữ liệu", null);
        } else {
            return new ResponseObject("200", "Đã tìm thấy dữ liệu", items);
        }
    }

    @GetMapping("superadmin/account/find-by-id/{id}")
    private UsersDto findById(@PathVariable int id) {
        Users user = usersService.findById(id);
        return EntityDtoUtils.convertToDto(user, UsersDto.class);
    }

    @PostMapping("superadmin/account/create-account")
    private void createAccount(@RequestBody DataAccountDto dataAccountDto) {
        AccountDto accountDto = dataAccountDto.getAccountDto();
        Users user = EntityDtoUtils.convertToEntity(accountDto, Users.class);

        List<String> roles = dataAccountDto.getRoles();
        List<Roles> rolesList = roles.stream().map(rolesService::findByNameRole).collect(Collectors.toList());

        user.setRoles(rolesList);
        user.setPassword(passwordEncoder.encode(accountDto.getPassword()));
        user.setAddress("Việt Nam");
        user.setDateCreated(new Timestamp(System.currentTimeMillis()));
        usersService.save(user);

        boolean containsAgentRole = roles.stream().anyMatch(role -> role.contains("ROLE_AGENT"));

        if (containsAgentRole) {
            Agencies agencies = new Agencies();
            agencies.setUserId(user.getId());
            agencies.setDateCreated(new Timestamp(System.currentTimeMillis()));
            agencies.setIsActive(Boolean.TRUE);
            agencies.setIsAccepted(0); // 0 là chưa kích hoạt, 1 chờ kích hoạt, 2 kích hoạt thành công, 3 kích hoạt thất bại
            agenciesService.save(agencies);

            registerBusiness(agencies.getId(), roles);

            emailService.queueEmailCreateBusiness(dataAccountDto);
        }
    }

    @PutMapping("superadmin/account/update-account")
    private void updateAccount(@RequestBody AccountDto accountDto) {
        Users users = usersService.findById(accountDto.getId());
        users.setFullName(accountDto.getFullName());
        users.setPhone(accountDto.getPhone());
        users.setIsActive(accountDto.getIsActive());
        users.setCitizenCard(accountDto.getCitizenCard());
        users.setAddress(accountDto.getAddress());
        usersService.save(users);
    }

    @DeleteMapping("superadmin/account/delete-account/{id}")
    private void deleteAccount(@PathVariable int id) {
        Users users = usersService.findById(id);
        users.setIsActive(Boolean.FALSE);
        usersService.save(users);
    }

    private void registerBusiness(int agenciesId, List<String> roles) {
        String hotelId = GenerateNextID.generateNextCode("HTL", hotelsService.findMaxCode());
        String transId = GenerateNextID.generateNextCode("TRP", transportationBrandsService.findMaxCode());
        String placeId = GenerateNextID.generateNextCode("PLA", visitLocationsService.findMaxCode());

        if (roles.contains("ROLE_AGENT_HOTEL")) {
            Hotels hotels = new Hotels();
            hotels.setId(hotelId);
            hotels.setHotelTypeId(1);
            hotels.setAgenciesId(agenciesId);
            hotels.setIsAccepted(Boolean.FALSE);
            hotels.setIsActive(Boolean.TRUE);
            hotels.setDateCreated(new Timestamp(System.currentTimeMillis()));
            hotelsService.save(hotels);
        }
        if (roles.contains("ROLE_AGENT_TRANSPORT")) {
            TransportationBrands transportationBrands = new TransportationBrands();
            transportationBrands.setId(transId);
            transportationBrands.setAgenciesId(agenciesId);
            transportationBrands.setIsAccepted(Boolean.FALSE);
            transportationBrands.setIsActive(Boolean.TRUE);
            transportationBrands.setDateCreated(new Timestamp(System.currentTimeMillis()));
            transportationBrandsService.save(transportationBrands);
        }
        if (roles.contains("ROLE_AGENT_PLACE")) {
            VisitLocations visitLocations = new VisitLocations();
            visitLocations.setId(placeId);
            visitLocations.setVisitLocationTypeId(1);
            visitLocations.setAgenciesId(agenciesId);
            visitLocations.setIsAccepted(Boolean.FALSE);
            visitLocations.setIsActive(Boolean.TRUE);
            visitLocations.setDateCreated(new Timestamp(System.currentTimeMillis()));
            visitLocationsService.save(visitLocations);
        }
    }
}
