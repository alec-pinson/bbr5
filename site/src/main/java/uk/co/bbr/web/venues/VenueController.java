package uk.co.bbr.web.venues;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.co.bbr.services.contests.ContestService;
import uk.co.bbr.services.contests.dao.ContestDao;
import uk.co.bbr.services.events.dao.ContestEventDao;
import uk.co.bbr.services.framework.NotFoundException;
import uk.co.bbr.services.venues.VenueService;
import uk.co.bbr.services.venues.dao.VenueAliasDao;
import uk.co.bbr.services.venues.dao.VenueDao;
import uk.co.bbr.services.venues.dto.VenueContestDto;
import uk.co.bbr.services.venues.dto.VenueContestYearDto;
import uk.co.bbr.web.security.annotations.IsBbrMember;
import uk.co.bbr.web.security.annotations.IsBbrPro;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final ContestService contestService;

    @GetMapping("/venues/{venueSlug:[\\-a-z\\d]{2,}}")
    public String venue(Model model, @PathVariable("venueSlug") String venueSlug) {
        Optional<VenueDao> venue = this.venueService.fetchBySlug(venueSlug);
        if (venue.isEmpty()) {
            throw NotFoundException.venueNotFoundBySlug(venueSlug);
        }
        List<VenueAliasDao> previousNames = this.venueService.fetchAliases(venue.get());

        List<VenueContestDto> venueContests = this.venueService.fetchVenueContests(venue.get());

        model.addAttribute("Venue", venue.get());
        model.addAttribute("PreviousNames", previousNames);
        model.addAttribute("VenueContests", venueContests);

        return "venues/venue";
    }

    @IsBbrPro
    @GetMapping("/venues/{venueSlug:[\\-a-z\\d]{2,}}/{contestSlug:[\\-a-z\\d]{2,}}")
    public String venueContest(Model model, @PathVariable("venueSlug") String venueSlug, @PathVariable("contestSlug") String contestSlug) {
        Optional<VenueDao> venue = this.venueService.fetchBySlug(venueSlug);
        if (venue.isEmpty()) {
            throw NotFoundException.venueNotFoundBySlug(venueSlug);
        }
        Optional<ContestDao> contest = this.contestService.fetchBySlug(contestSlug);
        if (contest.isEmpty()) {
            throw NotFoundException.contestNotFoundBySlug(contestSlug);
        }

        List<ContestEventDao> venueContests = this.venueService.fetchVenueContestEvents(venue.get(), contest.get());

        model.addAttribute("Venue", venue.get());
        model.addAttribute("VenueContests", venueContests);

        return "venues/contest";
    }

    @IsBbrPro
    @GetMapping("/venues/{venueSlug:[\\-a-z\\d]{2,}}/years")
    public String venueYears(Model model, @PathVariable("venueSlug") String venueSlug) {
        Optional<VenueDao> venue = this.venueService.fetchBySlug(venueSlug);
        if (venue.isEmpty()) {
            throw NotFoundException.venueNotFoundBySlug(venueSlug);
        }
        List<VenueAliasDao> previousNames = this.venueService.fetchAliases(venue.get());

        List<VenueContestYearDto> venueYears = this.venueService.fetchVenueContestYears(venue.get());

        model.addAttribute("Venue", venue.get());
        model.addAttribute("PreviousNames", previousNames);
        model.addAttribute("VenueYears", venueYears);

        return "venues/years";
    }

    @IsBbrPro
    @GetMapping("/venues/{venueSlug:[\\-a-z\\d]{2,}}/years/{year:\\d{4}}")
    public String venueYearEvents(Model model, @PathVariable("venueSlug") String venueSlug, @PathVariable("year") int year) {
        Optional<VenueDao> venue = this.venueService.fetchBySlug(venueSlug);
        if (venue.isEmpty()) {
            throw NotFoundException.venueNotFoundBySlug(venueSlug);
        }

        List<ContestEventDao> events = this.venueService.fetchVenueContestYear(venue.get(), year);

        model.addAttribute("Venue", venue.get());
        model.addAttribute("Year", Integer.toString(year));
        model.addAttribute("Events", events);

        return "venues/year";
    }
}
