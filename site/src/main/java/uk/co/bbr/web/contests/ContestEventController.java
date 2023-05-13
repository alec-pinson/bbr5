package uk.co.bbr.web.contests;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.co.bbr.services.contests.ContestEventService;
import uk.co.bbr.services.contests.ContestResultService;
import uk.co.bbr.services.contests.dao.ContestEventDao;
import uk.co.bbr.services.contests.dao.ContestResultDao;
import uk.co.bbr.services.framework.NotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ContestEventController {

    private final ContestEventService contestEventService;
    private final ContestResultService contestResultService;

    @GetMapping("/contests/{contestSlug:[\\-a-z\\d]{2,}}/{contestEventDate:\\d{4}-\\d{2}-\\d{2}}")
    public String contestEventDetails(Model model, @PathVariable String contestSlug, @PathVariable String contestEventDate) {
        String[] dateSplit = contestEventDate.split("-");
        LocalDate eventDate = LocalDate.of(Integer.parseInt(dateSplit[0]), Integer.parseInt(dateSplit[1]), Integer.parseInt(dateSplit[2]));
        Optional<ContestEventDao> contestEvent = this.contestEventService.fetchEvent(contestSlug, eventDate);

        if (contestEvent.isEmpty()) {
            throw new NotFoundException("Event with slug " + contestSlug + " and date " + contestEventDate + " not found");
        }

        List<ContestResultDao> eventResults = this.contestResultService.fetchForEvent(contestEvent.get());

        model.addAttribute("ContestEvent", contestEvent.get());
        model.addAttribute("EventResults", eventResults);

        return "contests/events/event";
    }



}
