package uk.co.bbr.services.people;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.bbr.services.bands.dao.BandDao;
import uk.co.bbr.services.contests.ContestResultService;
import uk.co.bbr.services.contests.repo.ContestAdjudicatorRepository;
import uk.co.bbr.services.framework.ValidationException;
import uk.co.bbr.services.framework.mixins.SlugTools;
import uk.co.bbr.services.people.dao.PersonAliasDao;
import uk.co.bbr.services.people.dao.PersonDao;
import uk.co.bbr.services.people.dto.PeopleListDto;
import uk.co.bbr.services.people.repo.PersonAliasRepository;
import uk.co.bbr.services.people.repo.PersonRepository;
import uk.co.bbr.services.people.sql.PeopleBandsSql;
import uk.co.bbr.services.people.sql.PeopleCountSql;
import uk.co.bbr.services.people.sql.PeopleWinnersSql;
import uk.co.bbr.services.people.sql.dto.PeopleBandsSqlDto;
import uk.co.bbr.services.people.sql.dto.PeopleWinnersSqlDto;
import uk.co.bbr.services.pieces.repo.PieceRepository;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.web.security.annotations.IsBbrAdmin;
import uk.co.bbr.web.security.annotations.IsBbrMember;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService, SlugTools {

    private final ContestResultService contestResultService;
    private final PersonRepository personRepository;
    private final PersonAliasRepository personAliasRepository;
    private final ContestAdjudicatorRepository contestAdjudicatorRepository;
    private final PieceRepository pieceRepository;
    private final SecurityService securityService;
    private final EntityManager entityManager;

    @Override
    @IsBbrMember
    public PersonDao create(PersonDao person) {
        return this.create(person, false);
    }

    @Override
    @IsBbrAdmin
    public PersonDao migrate(PersonDao person) {
        return this.create(person, true);
    }

    private PersonDao create(PersonDao person, boolean migrating) {
        this.validateMandatory(person);

        // validation
        if (person.getId() != null) {
            throw new ValidationException("Can't create with specific id");
        }

        // does the slug already exist?
        Optional<PersonDao> slugMatches = this.personRepository.fetchBySlug(person.getSlug());
        if (slugMatches.isPresent()) {
            throw new ValidationException("Person with slug " + person.getSlug() + " already exists.");
        }

        if (!migrating) {
            person.setCreated(LocalDateTime.now());
            person.setCreatedBy(this.securityService.getCurrentUsername());
            person.setUpdated(LocalDateTime.now());
            person.setUpdatedBy(this.securityService.getCurrentUsername());
        }
        return this.personRepository.saveAndFlush(person);
    }

    private void validateMandatory(PersonDao person) {
        if (StringUtils.isBlank(person.getSurname())) {
            throw new ValidationException("Person surname must be specified");
        }

        // defaults
        if (StringUtils.isBlank(person.getSlug())) {
            person.setSlug(slugify(person.getName()));
        }
    }

    @Override
    @IsBbrMember
    public PersonDao create(String surname, String firstNames) {
        PersonDao person = new PersonDao();
        person.setSurname(surname);
        person.setFirstNames(firstNames);
        return this.create(person);
    }

    @Override
    public PersonDao update(PersonDao person) {
        this.validateMandatory(person);

        // validation
        if (person.getId() == null) {
            throw new ValidationException("Can't update without an id");
        }

        // does the slug already exist?
        Optional<PersonDao> slugMatches = this.personRepository.fetchBySlug(person.getSlug());
        if (slugMatches.isPresent() && !slugMatches.get().getId().equals(person.getId())) {
            throw new ValidationException("Person with slug " + person.getSlug() + " already exists.");
        }

        person.setUpdated(LocalDateTime.now());
        person.setUpdatedBy(this.securityService.getCurrentUsername());
        return this.personRepository.saveAndFlush(person);
    }

    @Override
    @IsBbrMember
    public void createAlternativeName(PersonDao person, PersonAliasDao previousName) {
        this.createAlternativeName(person, previousName, false);
    }

    @Override
    @IsBbrAdmin
    public void migrateAlternativeName(PersonDao person, PersonAliasDao previousName) {
        this.createAlternativeName(person, previousName, true);
    }

    private void createAlternativeName(PersonDao person, PersonAliasDao previousName, boolean migrating) {
        previousName.setPerson(person);
        if (!migrating) {
            previousName.setCreated(LocalDateTime.now());
            previousName.setCreatedBy(this.securityService.getCurrentUsername());
            previousName.setUpdated(LocalDateTime.now());
            previousName.setUpdatedBy(this.securityService.getCurrentUsername());
        }
        this.personAliasRepository.saveAndFlush(previousName);
    }

    @Override
    public Optional<PersonDao> fetchBySlug(String personSlug) {
        return this.personRepository.fetchBySlug(personSlug);
    }

    @Override
    public Optional<PersonDao> fetchById(long personId) {
        return this.personRepository.fetchById(personId);
    }

    @Override
    public List<PersonAliasDao> findAllAliases(PersonDao person) {
        return this.personAliasRepository.findForPersonId(person.getId());
    }

    @Override
    public List<PersonAliasDao> findVisibleAliases(PersonDao person) {
        return this.personAliasRepository.findVisibleForPersonId(person.getId());
    }

    @Override
    public PeopleListDto listPeopleStartingWith(String prefix) {
        Map<Long,Integer> conductingCounts = PeopleCountSql.selectConductorCounts(this.entityManager);
        Map<Long,Integer> conductor2Counts = PeopleCountSql.selectConductorTwoCounts(this.entityManager);
        Map<Long,Integer> conductor3Counts = PeopleCountSql.selectConductorThreeCounts(this.entityManager);
        Map<Long,Integer> adjudicatorCounts = PeopleCountSql.selectAdjudicatorCounts(this.entityManager);
        Map<Long,Integer> composerCounts = PeopleCountSql.selectComposerCounts(this.entityManager);
        Map<Long,Integer> arrangerCounts = PeopleCountSql.selectArrangerCounts(this.entityManager);

        List<PersonDao> peopleToReturn;

        if (prefix.equalsIgnoreCase("ALL")) {
            peopleToReturn = this.personRepository.findAllOrderBySurname();
        } else {
            if (prefix.trim().length() != 1) {
                throw new UnsupportedOperationException("Prefix must be a single character");
            }
            peopleToReturn = this.personRepository.findByPrefixOrderBySurname(prefix.trim().toUpperCase());
        }

        // populate counts
        for (PersonDao eachPerson : peopleToReturn) {
            int conducting1 = conductingCounts.getOrDefault(eachPerson.getId(), 0);
            int conducting2 = conductor2Counts.getOrDefault(eachPerson.getId(), 0);
            int conducting3 = conductor3Counts.getOrDefault(eachPerson.getId(), 0);
            eachPerson.setConductingCount(conducting1 + conducting2 + conducting3);
            eachPerson.setAdjudicationsCount(adjudicatorCounts.getOrDefault(eachPerson.getId(), 0));
            eachPerson.setCompositionsCount(composerCounts.getOrDefault(eachPerson.getId(), 0));
            eachPerson.setArrangementsCount(arrangerCounts.getOrDefault(eachPerson.getId(), 0));
        }

        long allBandsCount = this.personRepository.count();

        return new PeopleListDto(peopleToReturn.size(), allBandsCount, prefix, peopleToReturn);
    }

    @Override
    public Optional<PersonAliasDao> aliasExists(PersonDao person, String aliasName) {
        String name = person.simplifyPersonFullName(aliasName);
        return this.personAliasRepository.fetchByNameForPerson(person.getId(), name);
    }

    @Override
    public int fetchAdjudicationCount(PersonDao person) {
        return this.contestAdjudicatorRepository.fetchAdjudicationCountForPerson(person.getId());
    }

    @Override
    public int fetchComposerCount(PersonDao person) {
        return this.pieceRepository.fetchComposerCountForPerson(person.getId());
    }

    @Override
    public int fetchArrangerCount(PersonDao person) {
        return this.pieceRepository.fetchArrangerCountForPerson(person.getId());
    }

    @Override
    public PersonDao findMatchingPersonByName(String personName, BandDao band, LocalDate dateContext) {
        Set<PersonDao> previousConductorsForThisBand = this.contestResultService.fetchBandConductors(band);

        for (PersonDao eachExistingConductor : previousConductorsForThisBand) {
            if (eachExistingConductor.matchesName(personName)) {
                return eachExistingConductor;
            }
        }

        Optional<PersonDao> matchingPerson = this.personRepository.fetchByCombinedName(personName.toUpperCase());

        if (matchingPerson.isPresent()) {
            return matchingPerson.get();
        }

        Optional<PersonAliasDao> matchingAlias = this.personAliasRepository.fetchByUpperName(personName.toUpperCase());
        return matchingAlias.map(PersonAliasDao::getPerson).orElse(null);

    }

    @Override
    public List<PeopleWinnersSqlDto> fetchContestWinningPeople() {
        return PeopleWinnersSql.selectWinningPeople(this.entityManager);
    }

    @Override
    public List<PeopleWinnersSqlDto> fetchContestWinningPeopleBefore(int year) {
        return PeopleWinnersSql.selectWinningPeopleBefore(this.entityManager, year);
    }

    @Override
    public List<PeopleWinnersSqlDto> fetchContestWinningPeopleAfter(int year) {
        return PeopleWinnersSql.selectWinningPeopleAfter(this.entityManager, year);
    }

    @Override
    public List<PeopleBandsSqlDto> fetchBandsConductedList() {
        return PeopleBandsSql.selectWinningPeople(this.entityManager);
    }

    @Override
    public List<PeopleBandsSqlDto> fetchBandsConductedListBefore(int year) {
        return PeopleBandsSql.selectWinningPeopleBefore(this.entityManager, year);
    }

    @Override
    public List<PeopleBandsSqlDto> fetchBandsConductedListAfter(int year) {
        return PeopleBandsSql.selectWinningPeopleAfter(this.entityManager, year);
    }


}
