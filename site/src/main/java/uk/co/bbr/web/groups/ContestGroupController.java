package uk.co.bbr.web.groups;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.co.bbr.services.groups.ContestGroupService;
import uk.co.bbr.services.groups.dao.ContestGroupAliasDao;
import uk.co.bbr.services.groups.dao.ContestGroupDao;
import uk.co.bbr.services.groups.dto.ContestGroupDetailsDto;
import uk.co.bbr.services.groups.dto.ContestGroupYearDto;
import uk.co.bbr.services.groups.dto.ContestGroupYearsDetailsDto;
import uk.co.bbr.services.events.dto.GroupListDto;
import uk.co.bbr.services.framework.NotFoundException;
import uk.co.bbr.web.security.annotations.IsBbrMember;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ContestGroupController {

    private final ContestGroupService contestGroupService;

    @GetMapping("/contest-groups")
    public String contestGroupsListHome(Model model) {
        return contestGroupsListLetter(model, "A");
    }

    @GetMapping("/contest-groups/{letter:[A-Z0-9]}")
    public String contestGroupsListLetter(Model model, @PathVariable("letter") String letter) {
        GroupListDto groups = this.contestGroupService.listGroupsStartingWith(letter);

        model.addAttribute("GroupPrefixLetter", letter);
        model.addAttribute("Groups", groups);
        return "groups/groups";
    }

    @GetMapping("/contest-groups/ALL")
    public String contestGroupsListAll(Model model) {
        GroupListDto groups = this.contestGroupService.listGroupsStartingWith("ALL");

        model.addAttribute("GroupPrefixLetter", "ALL");
        model.addAttribute("Groups", groups);
        return "groups/groups";
    }

    @GetMapping("/contest-groups/{groupSlug:[\\-A-Z\\d]{2,}}")
    public String contestGroupRedirect(@PathVariable("groupSlug") String groupSlug) {
        return "redirect:/contests/{groupSlug}";
    }

    @GetMapping("/contests/{slug:[\\-A-Z\\d]{2,}}")
    public String contestGroupDetails(Model model, @PathVariable("slug") String groupSlug) {
        Optional<ContestGroupDao> contestGroup = this.contestGroupService.fetchBySlug(groupSlug);
        if (contestGroup.isEmpty()) {
            throw NotFoundException.groupNotFoundBySlug(groupSlug);
        }

        ContestGroupDetailsDto contestGroupDetails = this.contestGroupService.fetchDetail(contestGroup.get());
        List<ContestGroupAliasDao> contestGroupAliases = this.contestGroupService.fetchAliases(contestGroup.get());

        model.addAttribute("Group", contestGroupDetails);
        model.addAttribute("PreviousNames", contestGroupAliases);
        return "groups/group";
    }

    @IsBbrMember
    @GetMapping("/contests/{slug:[\\-A-Z\\d]{2,}}/years")
    public String contestGroupYearDetails(Model model, @PathVariable("slug") String groupSlug) {
        Optional<ContestGroupDao> contestGroup = this.contestGroupService.fetchBySlug(groupSlug);
        if (contestGroup.isEmpty()) {
            throw NotFoundException.groupNotFoundBySlug(groupSlug);
        }

        ContestGroupYearsDetailsDto contestGroupDetails = this.contestGroupService.fetchYearsBySlug(groupSlug);
        List<ContestGroupAliasDao> contestGroupAliases = this.contestGroupService.fetchAliases(contestGroup.get());

        model.addAttribute("Group", contestGroupDetails);
        model.addAttribute("PreviousNames", contestGroupAliases);
        return "groups/years";
    }

    @IsBbrMember
    @GetMapping("/contests/{slug:[\\-A-Z\\d]{2,}}/{year:[0-9]{4}}")
    public String contestGroupYearDetails(Model model, @PathVariable("slug") String groupSlug, @PathVariable("year") Integer year) {
        ContestGroupYearDto eventsForGroupAndYear = this.contestGroupService.fetchEventsByGroupSlugAndYear(groupSlug, year);

        model.addAttribute("GroupYearEvents", eventsForGroupAndYear);
        return "groups/year";
    }

}
