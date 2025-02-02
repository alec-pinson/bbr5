package uk.co.bbr.web.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.co.bbr.services.events.ContestEventService;
import uk.co.bbr.services.events.ResultService;
import uk.co.bbr.services.events.dao.ContestEventDao;
import uk.co.bbr.services.events.dao.ContestEventTestPieceDao;
import uk.co.bbr.services.events.dao.ContestResultDao;
import uk.co.bbr.services.framework.NotFoundException;
import uk.co.bbr.services.security.UserService;
import uk.co.bbr.services.security.dao.SiteUserDao;
import uk.co.bbr.web.Tools;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ContestEventController {

    private final ContestEventService contestEventService;
    private final ResultService contestResultService;
    private final UserService userService;

    @GetMapping("/contests/{contestSlug:[\\-a-z\\d]{2,}}/{contestEventDate:\\d{4}-\\d{2}-\\d{2}}")
    public String contestEventDetails(Model model, @PathVariable String contestSlug, @PathVariable String contestEventDate) {
        LocalDate eventDate = Tools.parseEventDate(contestEventDate);
        Optional<ContestEventDao> contestEvent = this.contestEventService.fetchEvent(contestSlug, eventDate);

        if (contestEvent.isEmpty()) {
          contestEvent = this.contestEventService.fetchEventWithinWiderDateRange(contestSlug, eventDate);
          if (contestEvent.isEmpty()) {
            throw NotFoundException.eventNotFound(contestSlug, contestEventDate);
          }
        }

        List<ContestResultDao> eventResults = this.contestResultService.fetchForEvent(contestEvent.get());

        ContestEventDao nextEvent = this.contestEventService.fetchEventLinkNext(contestEvent.get());
        ContestEventDao previousEvent = this.contestEventService.fetchEventLinkPrevious(contestEvent.get());
        ContestEventDao upEvent = this.contestEventService.fetchEventLinkUp(contestEvent.get());
        ContestEventDao downEvent = this.contestEventService.fetchEventLinkDown(contestEvent.get());

        SiteUserDao contestOwner = null;
        Optional<SiteUserDao> contestOwnerOptional = this.userService.fetchUserByUsercode(contestEvent.get().getOwner());
        if (contestOwnerOptional.isPresent()) {
            contestOwner = contestOwnerOptional.get();
        } else {
            Optional<SiteUserDao> tjsOwnerOptional = this.userService.fetchUserByUsercode("tjs");
            if (tjsOwnerOptional.isPresent()) {
                contestOwner = tjsOwnerOptional.get();
            }
        }

        List<ContestEventTestPieceDao> eventTestPieces = this.contestEventService.listTestPieces(contestEvent.get());

        model.addAttribute("ContestEvent", contestEvent.get());
        model.addAttribute("EventTestPieces", eventTestPieces);
        model.addAttribute("ContestOwner", contestOwner);
        model.addAttribute("EventResults", eventResults);
        model.addAttribute("NextEvent", nextEvent);
        model.addAttribute("PreviousEvent", previousEvent);
        model.addAttribute("SectionUp", upEvent);
        model.addAttribute("SectionDown", downEvent);

        return "events/event";
    }
}
