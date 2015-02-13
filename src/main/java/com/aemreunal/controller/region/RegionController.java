package com.aemreunal.controller.region;

/*
 **************************
 * Copyright (c) 2014     *
 *                        *
 * This code belongs to:  *
 *                        *
 * Ahmet Emre Ünal        *
 * S001974                *
 *                        *
 * aemreunal@gmail.com    *
 * emre.unal@ozu.edu.tr   *
 *                        *
 * aemreunal.com          *
 **************************
 */

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.aemreunal.config.GlobalSettings;
import com.aemreunal.domain.Beacon;
import com.aemreunal.domain.Region;
import com.aemreunal.service.RegionService;

@Controller
@RequestMapping(GlobalSettings.REGION_PATH_MAPPING)
public class RegionController {
    @Autowired
    private RegionService regionService;

    /**
     * Get regions that belong to a project.
     *
     * @param projectId
     *         The ID of the project
     *
     * @return The list of regions that belong to the project with the specified ID
     */
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<Region>> viewRegionsOfProject(@PathVariable String username,
                                                             @PathVariable Long projectId,
                                                             @RequestParam(value = "name", required = false, defaultValue = "") String regionName) {
        if (regionName.equals("")) {
            List<Region> regions = regionService.getAllRegionsOf(username, projectId);
            return new ResponseEntity<List<Region>>(regions, HttpStatus.OK);
        } else {
            List<Region> regions = regionService.findRegionsBySpecs(username, projectId, regionName);
            return new ResponseEntity<List<Region>>(regions, HttpStatus.OK);
        }
    }

    /**
     * Get the region with specified ID
     *
     * @param projectId
     *         The ID of the project
     * @param regionId
     *         The ID of the region
     *
     * @return The region
     */
    @RequestMapping(method = RequestMethod.GET, value = GlobalSettings.REGION_ID_MAPPING, produces = "application/json")
    public ResponseEntity<Region> viewRegion(@PathVariable String username,
                                             @PathVariable Long projectId,
                                             @PathVariable Long regionId) {
        Region region = regionService.getRegion(username, projectId, regionId);
        // TODO add links
        return new ResponseEntity<Region>(region, HttpStatus.OK);
    }

    /**
     * Get beacons that belong to to the specified region
     *
     * @param projectId
     *         The ID of the project to operate in
     * @param regionId
     *         The ID of the region
     *
     * @return The list of beacons that belong to the region
     */
    // TODO maybe return just the list of Beacon IDs, queried from regions_to_beacon
    @RequestMapping(method = RequestMethod.GET, value = GlobalSettings.REGION_MEMBERS_MAPPING, produces = "application/json")
    public ResponseEntity<List<Beacon>> viewRegionMembers(@PathVariable String username,
                                                          @PathVariable Long projectId,
                                                          @PathVariable Long regionId) {
        List<Beacon> beaconList = regionService.getMembersOfRegion(username, projectId, regionId);
        return new ResponseEntity<List<Beacon>>(beaconList, HttpStatus.OK);
    }

    /**
     * Create a new region in project
     * <p>
     *
     * @param projectId
     *         The ID of the project to create the region in
     * @param regionJson
     *         The region as JSON object
     * @param builder
     *         The URI builder for post-creation redirect
     *
     * @return The created region
     */
    // TODO {@literal @}Transactional mark via http://stackoverflow.com/questions/11812432/spring-data-hibernate
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<Region> createRegionInProject(@PathVariable String username,
                                                        @PathVariable Long projectId,
                                                        @RequestBody Region regionJson,
                                                        UriComponentsBuilder builder) {
        Region savedRegion = regionService.save(username, projectId, regionJson);
        if (GlobalSettings.DEBUGGING) {
            System.out.println("Saved region with ID = \'" + savedRegion.getRegionId() +
                                       "\' name = \'" + savedRegion.getName() +
                                       "\' in project with ID = \'" + projectId + "\'");
        }

        return buildCreateResponse(username, builder, savedRegion);
    }

    private ResponseEntity<Region> buildCreateResponse(String username, UriComponentsBuilder builder, Region savedRegion) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path(GlobalSettings.REGION_SPECIFIC_MAPPING)
                                   .buildAndExpand(
                                           username,
                                           savedRegion.getProject().getProjectId(),
                                           savedRegion.getRegionId())
                                   .toUri());
        return new ResponseEntity<Region>(savedRegion, headers, HttpStatus.CREATED);
    }

    /**
     * Add beacon to the specified region.
     * <p>
     * Can return 409 if beacon already has a region.
     * <p>
     * Ex: "/Region/1/Add?beaconId=12"
     *
     * @param projectId
     *         The ID of the project to operate in
     * @param regionId
     *         The ID of the region to add the beacon to
     * @param beaconId
     *         The ID of the beacon to add
     *
     * @return The added region
     */
    @RequestMapping(method = RequestMethod.POST, value = GlobalSettings.REGION_ADD_MEMBER_MAPPING, produces = "application/json")
    public ResponseEntity<Region> addBeaconToRegion(@PathVariable String username,
                                                    @PathVariable Long projectId,
                                                    @PathVariable Long regionId,
                                                    @RequestParam(value = "beaconId", required = true) Long beaconId) {
        Region region = regionService.addBeaconToRegion(username, projectId, regionId, beaconId);
        return new ResponseEntity<Region>(region, HttpStatus.OK);
    }

    /**
     * Remove beacon from the specified region.
     * <p>
     * Can return 400 if beacon does not have a region.
     * <p>
     * Ex: "/Region/1/Remove?beaconId=12"
     *
     * @param projectId
     *         The ID of the project to operate in
     * @param regionId
     *         The ID of the region to remove the beacon from
     * @param beaconId
     *         The ID of the beacon to remove
     *
     * @return The removed region
     */
    @RequestMapping(method = RequestMethod.DELETE, value = GlobalSettings.REGION_REMOVE_MEMBER_MAPPING, produces = "application/json")
    public ResponseEntity<Region> removeBeaconFromRegion(@PathVariable String username,
                                                         @PathVariable Long projectId,
                                                         @PathVariable Long regionId,
                                                         @RequestParam(value = "beaconId", required = true) Long beaconId) {
        Region region = regionService.removeBeaconFromRegion(username, projectId, regionId, beaconId);
        return new ResponseEntity<Region>(region, HttpStatus.OK);
    }


    /**
     * Delete the specified region
     *
     * @param projectId
     *         The ID of the project to operate in
     * @param regionId
     *         The ID of the region to delete
     *
     * @return The deleted region
     */
    @RequestMapping(method = RequestMethod.DELETE, value = GlobalSettings.REGION_ID_MAPPING, produces = "application/json")
    public ResponseEntity<Region> deleteRegion(@PathVariable String username,
                                               @PathVariable Long projectId,
                                               @PathVariable Long regionId,
                                               @RequestParam(value = "confirm", required = true) String confirmation) {

        if (confirmation.toLowerCase().equals("yes")) {
            Region deletedRegion = regionService.delete(username, projectId, regionId);
            return new ResponseEntity<Region>(deletedRegion, HttpStatus.OK);
        } else {
            return new ResponseEntity<Region>(HttpStatus.PRECONDITION_FAILED);
        }
    }
}