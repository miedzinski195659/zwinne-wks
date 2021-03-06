package pl.lodz.p.it.wks.wksrecruiter.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.lodz.p.it.wks.wksrecruiter.collections.*;
import pl.lodz.p.it.wks.wksrecruiter.collections.questions.QuestionInfo;
import pl.lodz.p.it.wks.wksrecruiter.exceptions.WKSRecruiterException;
import pl.lodz.p.it.wks.wksrecruiter.repositories.AccountsRepository;
import pl.lodz.p.it.wks.wksrecruiter.repositories.TestsRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final AccountsRepository accountsRepository;

    private final TestsRepository testsRepository;

    @Autowired
    public AccountServiceImpl(BCryptPasswordEncoder bCryptPasswordEncoder, AccountsRepository accountsRepository, TestsRepository testsRepository) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.accountsRepository = accountsRepository;
        this.testsRepository = testsRepository;
    }

    private boolean checkIfRolesInEnum(Collection<String> roles) {
        AtomicBoolean rolesFlag = new AtomicBoolean(true);
        roles.forEach(role -> {
            if (!RolesEnum.getEnums().contains(role)) {
                rolesFlag.set(false);
            }
        });
        return rolesFlag.get();
    }

    private Account validateAccount(Account account, boolean isEdit) throws WKSRecruiterException {
        if (!checkIfRolesInEnum(account.getRoles())) {
            throw new WKSRecruiterException(new WKSRecruiterException.Error("ROLE_ERROR", "Wrong role name!"));
        }
        if (!isEdit) {
            validatePassword(account.getPassword());
        }
        return account;
    }

    private void validatePassword(String password) throws WKSRecruiterException {
        WKSRecruiterException ex = new WKSRecruiterException();
        if (password == null || password.equals("")) {
            ex.add(new WKSRecruiterException.Error("PASSWORD_EMPTY", "Password can not be empty!"));
        } else {
            if (!password.matches("^.\\S*")) {
                ex.add(new WKSRecruiterException.Error("PASSWORD_WHITE", "Password cannot contain whitespaces!"));
            }
            if (password.length() < 8 || password.length() > 16) {
                ex.add(new WKSRecruiterException.Error("PASSWORD_LENGTH", "Password have to contain between 8 and 16 characters!"));
            }
        }
        if (!ex.getErrors().isEmpty()) {
            throw ex;
        }
    }

    @Override
    public Account createAccount(Account account) throws WKSRecruiterException {
        try {
            validateAccount(account, false).setPassword(bCryptPasswordEncoder.encode(account.getPassword()));
            account.setSolvedTests(new ArrayList<>());
            account.setEnabled(Boolean.TRUE);
            accountsRepository.save(account);
            account.setPassword(null);
            return account;
        } catch (DuplicateKeyException exc) {
            throw WKSRecruiterException.createException("LOGIN_NOT_UNIQUE",
                    "Account with such login already exists. Try another one.");
        }
    }

    @Override
    public Account editRoles(String login, Collection<String> roles) throws WKSRecruiterException {
        Optional<Account> account = accountsRepository.findByLogin(login);
        if (account.isPresent()) {
            if (!checkIfRolesInEnum(roles)) {
                throw WKSRecruiterException.createException("ROLE_ERROR", "Wrong role name!");
            }
            account.get().setRoles(roles);
            accountsRepository.save(account.get());
            account.get().setPassword(null);
            return account.get();
        } else {
            throw WKSRecruiterException.createAccountNotFoundException();
        }
    }

    @Override
    public Account editAccount(Account account) throws WKSRecruiterException {
        validateAccount(account, true);
        Optional<Account> accountToEdit = accountsRepository.findByLogin(account.getLogin());
        if (accountToEdit.isPresent()) {
            accountToEdit.get().setName(account.getName());
            accountToEdit.get().setSurname(account.getSurname());
            if (account.getPassword() != null && !account.getPassword().isEmpty()) {
                validatePassword(account.getPassword());
                accountToEdit.get().setPassword(bCryptPasswordEncoder.encode(account.getPassword()));
            }
            accountToEdit.get().setRoles(account.getRoles());
            accountsRepository.save(accountToEdit.get());
            accountToEdit.get().setPassword(null);
            return accountToEdit.get();
        } else {
            throw WKSRecruiterException.createAccountNotFoundException();
        }
    }

    @Override
    public Account deleteAccount(String login) throws WKSRecruiterException {
        Optional<Account> account = accountsRepository.findByLogin(login);
        if (account.isPresent()) {
            account.get().setEnabled(false);
            accountsRepository.save(account.get());
            return account.get();
        } else {
            throw new WKSRecruiterException(new WKSRecruiterException.Error("ACCOUNT_NOT_FOUND", "Account with such login does not exist."));
        }
    }

    @Override
    public List<Account> getAll() {
        List<Account> accounts = accountsRepository.findAll();
        accounts = accounts.stream().filter(Account::getEnabled).collect(Collectors.toList());
        accounts.forEach(account -> account.setPassword(null));
        return accounts;
    }

    @Override
    public Account register(Account account) throws WKSRecruiterException {
        account.setRoles(Collections.singletonList(RolesEnum.CAN.toString()));
        return createAccount(account);
    }

    @Override
    public Account addSolveTest(String login, TestAttempt testAttempt) throws WKSRecruiterException {
        Optional<Account> account = accountsRepository.findByLogin(login);
        if (account.isPresent()) {
            if (account.get().getSolvedTests() != null) {
                if (account.get().getSolvedTests().contains(testAttempt)) {
                    throw WKSRecruiterException.createTestAlreadySolvedException();
                }
            }
            Optional<Test> solvedTest = testsRepository.findById(testAttempt.getTest().getId());
            if (solvedTest.isPresent()) {
                QuestionInfo[] originalQuestions = solvedTest.get().getQuestions()
                        .toArray(new QuestionInfo[solvedTest.get().getQuestions().size()]);
                AttemptAnswer[] attemptAnswers = testAttempt.getAnswers()
                        .toArray(new AttemptAnswer[testAttempt.getAnswers().size()]);
                if (originalQuestions.length != attemptAnswers.length)
                    throw WKSRecruiterException.createTestNotFoundException();
                int pointsSum = 0;
                for (int i = 0; i < originalQuestions.length; i++) {
                    if (!attemptAnswers[i].getQuestion().equals(originalQuestions[i].getQuestionPhrase()))
                        throw WKSRecruiterException.createTestNotFoundException();
                    attemptAnswers[i].setMaxPoints(originalQuestions[i].getMaxPoints());
                    pointsSum += originalQuestions[i].getMaxPoints();
                    attemptAnswers[i].setScore(-1);
                }
                testAttempt.setMaxPoints(pointsSum);
                testAttempt.setAnswers(Arrays.asList(attemptAnswers));
            } else throw WKSRecruiterException.createTestNotFoundException();
            testAttempt.setScore(TestAttempt.SOLVED_UNCHECKED); //temporary status
            account.get().getSolvedTests().add(testAttempt);
            return accountsRepository.save(account.get());
        } else {
            throw WKSRecruiterException.createAccountNotFoundException();
        }
    }
}
