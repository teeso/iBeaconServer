package com.aemreunal.service;

/*
 ***************************
 * Copyright (c) 2014      *
 *                         *
 * This code belongs to:   *
 *                         *
 * @author Ahmet Emre Ünal *
 * S001974                 *
 *                         *
 * aemreunal@gmail.com     *
 * emre.unal@ozu.edu.tr    *
 *                         *
 * aemreunal.com           *
 ***************************
 */

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aemreunal.config.GlobalSettings;
import com.aemreunal.domain.Beacon;
import com.aemreunal.domain.Project;
import com.aemreunal.domain.Region;
import com.aemreunal.domain.Scenario;
import com.aemreunal.exception.scenario.*;
import com.aemreunal.repository.scenario.ScenarioRepo;

@Transactional
@Service
public class ScenarioService {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private BeaconService beaconService;

    @Autowired
    private RegionService regionService;

    @Autowired
    private ScenarioRepo scenarioRepo;

    public Scenario save(String username, Long projectId, Scenario scenario) throws ConstraintViolationException {
        if (GlobalSettings.DEBUGGING) {
            System.out.println("Saving scenario with ID = \'" + scenario.getScenarioId() + "\'");
        }
        // Even though the 'project' variable is only used inside the if-clause,
        // the Project is found no matter what to ensure it exists and legitimate.
        Project project = projectService.findProjectById(username, projectId);
        if (scenario.getProject() == null) {
            // This means it hasn't been saved yet
            scenario.setProject(project);
        }
        return scenarioRepo.save(scenario);
    }

    public List<Scenario> getScenariosOfProject(String username, Long projectId) {
        Project project = projectService.findProjectById(username, projectId);
        List<Scenario> scenarios = project.getScenarios().stream().collect(Collectors.toList());
        return scenarios;
    }

    public Scenario getScenario(String username, Long projectId, Long scenarioId) throws ScenarioNotFoundException {
        if (GlobalSettings.DEBUGGING) {
            System.out.println("Finding scenario with ID = \'" + scenarioId + "\' in project = \'" + projectId + "\'");
        }
        Project project = projectService.findProjectById(username, projectId);
        Scenario scenario = scenarioRepo.findByScenarioIdAndProject(scenarioId, project);
        if (scenario == null) {
            throw new ScenarioNotFoundException(scenarioId);
        }
        return scenario;
    }

    public Scenario queryForScenario(String uuid, Integer major, Integer minor, String projectSecret)
    throws NoScenarioForQueryException {
        Beacon beacon = beaconService.queryForBeacon(uuid, major, minor, projectSecret);
        Scenario scenario;
        // TODO fix for 'beacon in region' update
        if (beacon.getRegion() != null) {
            scenario = beacon.getRegion().getScenario();
        } else {
            scenario = beacon.getScenario();
        }
        if (scenario == null) {
            throw new NoScenarioForQueryException(uuid, major, minor);
        }
        return scenario;
    }

    public Set<Beacon> getBeaconsInScenario(String username, Long projectId, Long scenarioId) {
        Scenario scenario = this.getScenario(username, projectId, scenarioId);
        return scenario.getBeacons().stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Region> getRegionsInScenario(String username, Long projectId, Long scenarioId) {
        Scenario scenario = this.getScenario(username, projectId, scenarioId);
        return scenario.getRegions().stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Scenario delete(String username, Long projectId, Long scenarioId) {
        if (GlobalSettings.DEBUGGING) {
            System.out.println("Deleting scenario with ID = \'" + scenarioId + "\'");
        }
        Scenario scenario = this.getScenario(username, projectId, scenarioId);
        removeAllRegionsFromScenario(username, projectId, scenario);
        removeAllBeaconsFromScenario(username, projectId, scenario);
        scenarioRepo.delete(scenario);
        return scenario;
    }

    private void removeAllRegionsFromScenario(String username, Long projectId, Scenario scenario) {
        for (Region region : scenario.getRegions()) {
            removeRegionFromScenario(username, projectId, scenario.getScenarioId(),region);
        }
    }

    private void removeAllBeaconsFromScenario(String username, Long projectId, Scenario scenario) {
        for (Beacon beacon : scenario.getBeacons()) {
            removeBeaconFromScenario(username, projectId, scenario.getScenarioId(), beacon);
        }
    }

    public Beacon addBeaconToScenario(String username, Long projectId, Long scenarioId, Long beaconId)
    throws BeaconHasScenarioException, BeaconWithRegionScenarioException {
        Beacon beacon = beaconService.getBeacon(username, projectId, beaconId);
        return addBeaconToScenario(username, projectId, scenarioId, beacon);
    }

    private Beacon addBeaconToScenario(String username, Long projectId, Long scenarioId, Beacon beacon) {
        Scenario scenario = getScenario(username, projectId, scenarioId);
        if (beacon.getRegion() != null) {
            throw new BeaconWithRegionScenarioException(beacon.getBeaconId(), beacon.getRegion().getRegionId());
        }
        if (beacon.getScenario() != null) {
            throw new BeaconHasScenarioException(beacon.getBeaconId(), beacon.getScenario().getScenarioId());
        }
        beaconService.setBeaconScenario(username, projectId, beacon, scenario, this);
        return beacon;
    }

    public Beacon removeBeaconFromScenario(String username, Long projectId, Long scenarioId, Long beaconId)
    throws BeaconDoesNotHaveScenarioException, BeaconHasScenarioException, BeaconWithRegionScenarioException {
        Beacon beacon = beaconService.getBeacon(username, projectId, beaconId);
        removeBeaconFromScenario(username, projectId, scenarioId, beacon);
        return beacon;
    }

    public Beacon removeBeaconFromScenario(String username, Long projectId, Long scenarioId, Beacon beacon) {
        if (beacon.getRegion() != null) {
            throw new BeaconWithRegionScenarioException(beacon.getBeaconId(), beacon.getRegion().getRegionId());
        }
        if (beacon.getScenario() == null) {
            throw new BeaconDoesNotHaveScenarioException(beacon.getBeaconId(), scenarioId);
        } else if (!(beacon.getScenario().getScenarioId().equals(scenarioId))) {
            throw new BeaconHasScenarioException(beacon.getBeaconId(), beacon.getScenario().getScenarioId());
        }
        beaconService.setBeaconScenario(username, projectId, beacon, null, this);
        return beacon;
    }

    public Region addRegionToScenario(String username, Long projectId, Long scenarioId, Long regionId)
    throws RegionHasScenarioException {
        Region region = regionService.getRegion(username, projectId, regionId);
        return addRegionToScenario(username, projectId, scenarioId, region);
    }

    public Region addRegionToScenario(String username, Long projectId, Long scenarioId, Region region) {
        Scenario scenario = getScenario(username, projectId, scenarioId);
        if (region.getScenario() != null) {
            throw new RegionHasScenarioException(region.getRegionId(), region.getScenario().getScenarioId());
        }
        regionService.setRegionScenario(username, projectId, region, scenario);
        return region;
    }

    public Region removeRegionFromScenario(String username, Long projectId, Long scenarioId, Long regionId)
    throws RegionHasScenarioException, RegionDoesNotHaveScenarioException {
        Region region = regionService.getRegion(username, projectId, regionId);
        return removeRegionFromScenario(username, projectId, scenarioId, region);
    }

    public Region removeRegionFromScenario(String username, Long projectId, Long scenarioId, Region region) {
        if (region.getScenario() == null) {
            throw new RegionDoesNotHaveScenarioException(region.getRegionId(), scenarioId);
        } else if (!(region.getScenario().getScenarioId().equals(scenarioId))) {
            throw new RegionHasScenarioException(region.getRegionId(), region.getScenario().getScenarioId());
        }
        regionService.setRegionScenario(username, projectId, region, null);
        return region;
    }

}
