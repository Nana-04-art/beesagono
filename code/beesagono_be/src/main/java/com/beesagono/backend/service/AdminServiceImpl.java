package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.CreateAdminRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStatResponse;
import com.beesagono.backend.dto.puzzle.PuzzleAdminResponse;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleWordsRequest;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.InvalidWordAttempt;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserRole;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
import com.beesagono.backend.entity.id.PuzzleWordId;
import com.beesagono.backend.entity.id.UserRoleId;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.mapper.UserMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.InvalidWordAttemptRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import com.beesagono.backend.repository.RoleRepository;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final UserRoleRepository userRoleRepository;
        private final PasswordEncoder passwordEncoder;
        private final UserMapper userMapper;

        private final DailyPuzzleRepository dailyPuzzleRepository;
        private final InvalidWordAttemptRepository invalidWordAttemptRepository;
        private final DictionaryWordRepository dictionaryWordRepository;
        private final GameSessionRepository gameSessionRepository;
        private final PuzzleWordRepository puzzleWordRepository;

        private final PuzzleGeneratorService puzzleGeneratorService;

        // -- User Managment --

        @Override
        @Transactional
        public UserResponse createAdmin(CreateAdminRequest request) {
                if (userRepository.existsByUsername(request.getUsername())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username già in uso.");
                }

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email già in uso.");
                }

                Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Ruolo ADMIN non trovato."));

                Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                                .orElseThrow(
                                                () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                                "Ruolo USER non trovato."));

                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .userRoles(new ArrayList<>())
                                .build();

                User savedUser = userRepository.saveAndFlush(user);

                UserRole userRoleRel = UserRole.builder()
                                .id(new UserRoleId(savedUser.getId(), userRole.getId()))
                                .user(savedUser)
                                .role(userRole)
                                .build();

                UserRole adminRoleRel = UserRole.builder()
                                .id(new UserRoleId(savedUser.getId(), adminRole.getId()))
                                .user(savedUser)
                                .role(adminRole)
                                .build();

                userRoleRepository.save(userRoleRel);
                userRoleRepository.save(adminRoleRel);

                savedUser.getUserRoles().add(userRoleRel);
                savedUser.getUserRoles().add(adminRoleRel);

                return userMapper.toUserResponse(savedUser);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<UserResponse> getUsers(String search, Pageable pageable) {
                if (search != null && !search.isBlank()) {
                        return userRepository
                                        .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search,
                                                        pageable)
                                        .map(userMapper::toUserResponse);
                }
                return userRepository.findAll(pageable).map(userMapper::toUserResponse);
        }

        // -- Puzzle Managment & Inspection --

        @Override
        @Transactional
        public PuzzleAdminResponse generateOrResetFuturePuzzle(LocalDate date) {
                validateFutureDate(date);

                dailyPuzzleRepository.findByPuzzleDate(date).ifPresent(dailyPuzzleRepository::delete);
                DailyPuzzle newPuzzle = puzzleGeneratorService.generateAndSavePuzzleForDate(date);
                return mapToPuzzleAdminResponse(newPuzzle);
        }

        @Override
        @Transactional(readOnly = true)
        public PuzzleAdminResponse getPuzzleDetailsByDate(LocalDate date) {
                DailyPuzzle puzzle = dailyPuzzleRepository.findByPuzzleDate(date)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Nessun puzzle trovato per la data: " + date));
                return mapToPuzzleAdminResponse(puzzle);
        }

        @Override
        @Transactional(readOnly = true)
        public List<PuzzleAdminResponse> getAllPuzzlesOverview() {
                return dailyPuzzleRepository.findAllByOrderByPuzzleDateDesc().stream()
                                .map(this::mapToPuzzleAdminResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public PuzzleAdminResponse updatePuzzleWords(String puzzleId, UpdatePuzzleWordsRequest request) {
                DailyPuzzle puzzle = getPuzzleAndValidateEditable(puzzleId);

                // Remove Words
                if (request.getWordsToRemove() != null && !request.getWordsToRemove().isEmpty()) {
                        List<String> toRemoveUpper = request.getWordsToRemove().stream()
                                        .map(String::toUpperCase)
                                        .toList();

                        List<PuzzleWord> wordsToDelete = puzzle.getPuzzleWords().stream()
                                        .filter(pw -> toRemoveUpper
                                                        .contains(pw.getDictionaryWord().getWord().toUpperCase()))
                                        .toList();

                        puzzle.getPuzzleWords().removeAll(wordsToDelete);
                        puzzleWordRepository.deleteAll(wordsToDelete);
                }

                // Add Words
                if (request.getWordsToAdd() != null && !request.getWordsToAdd().isEmpty()) {

                        // Extraction of the set of allowed letters from the puzzle grid
                        String centerLetter = puzzle.getCenterLetter().toUpperCase();
                        Set<Character> allowedLetters = new HashSet<>();
                        allowedLetters.add(centerLetter.charAt(0));

                        if (puzzle.getOuterLetters() != null) {
                                puzzle.getOuterLetters().forEach(pol -> allowedLetters
                                                .add(pol.getId().getLetter().toUpperCase().charAt(0)));
                        }

                        for (String wordStr : request.getWordsToAdd()) {
                                String cleanWord = wordStr.trim().toUpperCase();

                                // Check for presence in the general dictionary
                                DictionaryWord dictWord = dictionaryWordRepository.findById(cleanWord)
                                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                "La parola '" + cleanWord
                                                                                + "' non esiste nel dizionario generale."));

                                // Check formal validity against the puzzle letters
                                if (!cleanWord.contains(centerLetter)) {
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "La parola '" + cleanWord
                                                                        + "' non contiene la lettera centrale obbligatoria ("
                                                                        + centerLetter + ").");
                                }

                                for (char c : cleanWord.toCharArray()) {
                                        if (!allowedLetters.contains(c)) {
                                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                "La parola '" + cleanWord
                                                                                + "' contiene il carattere non ammesso '"
                                                                                + c + "'.");
                                        }
                                }

                                // Check for duplicates in the puzzle
                                boolean alreadyPresent = puzzle.getPuzzleWords().stream()
                                                .anyMatch(pw -> pw.getDictionaryWord().getWord()
                                                                .equalsIgnoreCase(cleanWord));

                                if (!alreadyPresent) {
                                        boolean isMielegramma = dictWord.getUniqueLettersCount() == 7;

                                        PuzzleWord newPw = PuzzleWord.builder()
                                                        .id(new PuzzleWordId(puzzle.getId(), cleanWord))
                                                        .puzzle(puzzle)
                                                        .dictionaryWord(dictWord)
                                                        .isMielegramma(isMielegramma)
                                                        .build();

                                        puzzle.getPuzzleWords().add(newPw);
                                        puzzleWordRepository.save(newPw);
                                }
                        }
                }

                // Recalculate the updated maximum score based on the words present
                int updatedMaxScore = puzzle.getPuzzleWords().stream()
                                .mapToInt(pw -> {
                                        String w = pw.getDictionaryWord().getWord();
                                        int base = w.length() == 4 ? 1 : w.length();
                                        int bonus = Boolean.TRUE.equals(pw.getIsMielegramma()) ? 7 : 0;
                                        return base + bonus;
                                }).sum();

                puzzle.setMaxScore(updatedMaxScore);
                DailyPuzzle updated = dailyPuzzleRepository.save(puzzle);

                return mapToPuzzleAdminResponse(updated);
        }

        @Override
        @Transactional
        public PuzzleAdminResponse updatePuzzleLetters(String puzzleId, UpdatePuzzleLettersRequest request) {
                DailyPuzzle puzzle = getPuzzleAndValidateEditable(puzzleId);

                puzzle.setCenterLetter(request.getCenterLetter().toUpperCase());

                // Mapping outer letters
                List<PuzzleOuterLetter> outerLettersList = request.getOuterLetters().stream()
                                .map(letter -> PuzzleOuterLetter.builder()
                                                .id(new PuzzleOuterLetterId(puzzle.getId(), letter.toUpperCase()))
                                                .puzzle(puzzle)
                                                .build())
                                .collect(Collectors.toList());

                puzzle.getOuterLetters().clear();
                puzzle.getOuterLetters().addAll(outerLettersList);

                puzzleGeneratorService.recalculatePuzzleWords(puzzle);

                DailyPuzzle updated = dailyPuzzleRepository.save(puzzle);
                return mapToPuzzleAdminResponse(updated);
        }

        // -- Audit & Dictionary --

        @Override
        @Transactional(readOnly = true)
        public List<InvalidWordAttemptStatResponse> getTopSuggestedWordsFromAttempts() {
                // Retrieve rejected attempts for 'NOT_IN_DICTIONARY' and group them in
                // Java for maximum compatibility
                List<InvalidWordAttempt> attempts = invalidWordAttemptRepository.findAll().stream()
                                .filter(a -> ErrorTypeCode.NOT_IN_DICTIONARY.equals(a.getErrorReason()))
                                .toList();

                return attempts.stream()
                                .collect(Collectors.groupingBy(InvalidWordAttempt::getAttemptedWord,
                                                Collectors.counting()))
                                .entrySet().stream()
                                .map(entry -> new InvalidWordAttemptStatResponse(entry.getKey(), entry.getValue()))
                                .sorted(Comparator.comparing(InvalidWordAttemptStatResponse::getAttemptCount)
                                                .reversed())
                                .toList();
        }

        @Override
        @Transactional
        public void removeWordFromDictionary(String word) {
                String cleanWord = word.trim().toUpperCase();
                DictionaryWord dictionaryWord = dictionaryWordRepository.findById(cleanWord)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Parola non trovata nel dizionario: " + cleanWord));

                // Removes the word from the puzzles where it is used before deleting it from the dictionary
                puzzleWordRepository.deleteByIdWord(cleanWord);

                dictionaryWordRepository.delete(dictionaryWord);
        }

        // -- Private Helper Methods --

        private DailyPuzzle getPuzzleAndValidateEditable(String puzzleId) {
                DailyPuzzle puzzle = dailyPuzzleRepository.findById(puzzleId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Puzzle non trovato con ID: " + puzzleId));
                validateFutureDate(puzzle.getPuzzleDate());
                return puzzle;
        }

        private void validateFutureDate(LocalDate date) {
                if (!date.isAfter(LocalDate.now())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Operazione consentita solo per i puzzle futuri (data > " + LocalDate.now()
                                                        + ")");
                }
        }

        private PuzzleAdminResponse mapToPuzzleAdminResponse(DailyPuzzle puzzle) {
                boolean isEditable = puzzle.getPuzzleDate().isAfter(LocalDate.now());
                Long activeSessions = gameSessionRepository.countByPuzzleIdAndIsCompletedFalse(puzzle.getId());
                Long completedSessions = gameSessionRepository.countByPuzzleIdAndIsCompletedTrue(puzzle.getId());

                Set<String> outerLetters = puzzle.getOuterLetters() != null
                                ? puzzle.getOuterLetters().stream()
                                                .map(pol -> pol.getId().getLetter())
                                                .collect(Collectors.toSet())
                                : Set.of();

                List<String> validWords = puzzle.getPuzzleWords() != null
                                ? puzzle.getPuzzleWords().stream().map(pw -> pw.getDictionaryWord().getWord())
                                                .collect(Collectors.toList())
                                : List.of();

                return PuzzleAdminResponse.builder()
                                .id(puzzle.getId())
                                .puzzleDate(puzzle.getPuzzleDate())
                                .centerLetter(puzzle.getCenterLetter())
                                .outerLetters(outerLetters)
                                .maxScore(puzzle.getMaxScore())
                                .editable(isEditable)
                                .validWords(validWords)
                                .activeSessionsCount(activeSessions)
                                .completedSessionsCount(completedSessions)
                                .build();
        }
}