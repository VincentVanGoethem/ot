package dev.danvega.ot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;

    public DataLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Loading test data...");

        userRepository.save(new User("Dan", "Welcome back, Dan!"));
        userRepository.save(new User("Alice", "Good to see you, Alice!"));
        userRepository.save(new User("Bob", "Hey there, Bob!"));

        log.info("Loaded {} users", userRepository.count());
    }
}
