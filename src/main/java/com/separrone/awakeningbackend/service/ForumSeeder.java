package com.separrone.awakeningbackend.service;

import com.separrone.awakeningbackend.entity.Category;
import com.separrone.awakeningbackend.entity.Post;
import com.separrone.awakeningbackend.entity.Thread;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.repository.CategoryRepository;
import com.separrone.awakeningbackend.repository.PostRepository;
import com.separrone.awakeningbackend.repository.ThreadRepository;
import com.separrone.awakeningbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Order(2) // Run after UserSeeder
public class ForumSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    // Sample data arrays
    private final String[] usernames = {
        "GameMaster", "PixelWarrior", "ShadowHunter", "MysticSage", "CodeNinja", "DragonSlayer",
        "CyberKnight", "NeonPhoenix", "VoidWalker", "StarSeeker", "RuneScribe", "IronFist",
        "SilverArrow", "GoldRush", "DarkMage", "LightBringer", "StormRider", "EarthShaker",
        "FireStorm", "IceQueen", "WindDancer", "ThunderBolt", "MoonLight", "SunBurst",
        "CosmicRay", "QuantumLeap", "DataMiner", "ByteCrusher", "LogicBomb", "BugHunter",
        "FeatureMaster", "LoopBreaker", "RecursiveKing", "AsyncQueen", "PromiseKeeper",
        "CallbackHero", "EventDriven", "StateManager", "PropsDriller", "HookMaster",
        "ComponentWiz", "EffectHandler", "MemoryLeaker", "PerformanceGuru", "OptimizeThis",
        "DebugNinja", "TestRunner", "MockMaster", "StubBuilder", "SpyWatcher"
    };

    private final String[] categoryNames = {
        "General Discussion", "Game Development", "Technical Support", "Bug Reports", "Feature Requests"
    };

    private final String[] categoryDescriptions = {
        "General discussions about anything and everything",
        "Share your game development progress, tips, and tricks",
        "Need help? Ask for technical support here",
        "Report bugs and issues you've encountered",
        "Suggest new features and improvements"
    };

    private final String[] threadTitles = {
        "Welcome to the community!", "How to get started?", "Best practices discussion",
        "Tips and tricks for beginners", "Advanced techniques", "Common issues and solutions",
        "Performance optimization guide", "Latest updates and news", "Community guidelines",
        "Feedback and suggestions", "Troubleshooting help", "Tutorial request",
        "Code review needed", "Architecture discussion", "Design patterns",
        "Database optimization", "Security best practices", "Testing strategies",
        "Deployment issues", "Scaling challenges", "New feature preview",
        "Beta testing signup", "Version compatibility", "Migration guide",
        "API documentation", "Integration help", "Third-party tools",
        "Development workflow", "Team collaboration", "Project showcase"
    };

    private final String[] postContents = {
        "Thanks for the great discussion everyone! This has been really helpful.",
        "I had the same issue and found that updating the configuration solved it.",
        "Here's a code snippet that might help:\n\n```\nfunction example() {\n  console.log('Hello World');\n}\n```",
        "Great point! I hadn't considered that approach before.",
        "This is exactly what I was looking for. Thank you for sharing!",
        "I'm experiencing a similar problem. Has anyone found a solution?",
        "The documentation could be clearer on this topic. Maybe we should update it?",
        "I've been working on this for hours. Your solution saved me so much time!",
        "Could you provide more details about your setup? That would help with debugging.",
        "I think there might be a better way to handle this. What do you think?",
        "This feature would be amazing! I hope it gets implemented soon.",
        "I've tested this thoroughly and can confirm it works as expected.",
        "Make sure to check the logs for any error messages. That usually helps.",
        "The latest version has some breaking changes. Here's how to migrate:",
        "I love how active this community is! Everyone is so helpful.",
        "This is a known issue. The fix should be available in the next release.",
        "Performance has been great for me. Are you using the recommended settings?",
        "I created a demo project that might help illustrate the concept.",
        "The new UI looks fantastic! Great work on the design improvements.",
        "I'm getting different results on my machine. Could it be environment-specific?",
        "Here's my take on this problem. Let me know what you think!",
        "I've documented the entire process in case others run into this.",
        "The community has grown so much since I joined. It's amazing to see!",
        "I'm working on a similar project. Would love to collaborate!",
        "This thread has been incredibly informative. Thanks to everyone who contributed!"
    };

    public ForumSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      ThreadRepository threadRepository, PostRepository postRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Only seed if categories don't exist
        if (categoryRepository.count() == 0) {
            System.out.println("🌱 Starting forum data seeding...");
            
            seedUsers();
            seedCategories();
            seedThreadsAndPosts();
            
            System.out.println("✅ Forum seeding completed successfully!");
        } else {
            System.out.println("📊 Forum data already exists, skipping seeding.");
        }
    }

    private void seedUsers() {
        System.out.println("👥 Creating 50 dummy users...");
        
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            User user = new User();
            user.setUsername(usernames[i]);
            user.setEmail(usernames[i].toLowerCase() + "@example.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setEnabled(true);
            users.add(user);
        }
        
        userRepository.saveAll(users);
        System.out.println("✅ Created 50 users");
    }

    private void seedCategories() {
        System.out.println("📁 Creating 5 forum categories...");
        
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Category category = new Category(
                categoryNames[i],
                categoryDescriptions[i],
                i + 1
            );
            categories.add(category);
        }
        
        categoryRepository.saveAll(categories);
        System.out.println("✅ Created 5 categories");
    }

    private void seedThreadsAndPosts() {
        System.out.println("🧵 Creating threads and posts...");
        
        List<User> users = userRepository.findAll();
        List<Category> categories = categoryRepository.findAll();
        
        int totalThreads = 0;
        int totalPosts = 0;
        
        for (Category category : categories) {
            System.out.println("Creating 100 threads for category: " + category.getName());
            
            for (int threadNum = 0; threadNum < 100; threadNum++) {
                // Create thread
                User threadCreator = users.get(random.nextInt(users.size()));
                String threadTitle = getRandomThreadTitle() + " #" + (threadNum + 1);
                
                Thread thread = new Thread(threadTitle, category, threadCreator);
                // Randomize creation time within last 30 days
                LocalDateTime createdTime = LocalDateTime.now().minusDays(random.nextInt(30));
                thread.setCreatedAt(createdTime);
                thread.setLastPostAt(createdTime);
                
                // Randomly pin some threads (5% chance)
                if (random.nextDouble() < 0.05) {
                    thread.setIsPinned(true);
                }
                
                // Randomly lock some threads (2% chance)
                if (random.nextDouble() < 0.02) {
                    thread.setIsLocked(true);
                }
                
                thread = threadRepository.save(thread);
                totalThreads++;
                
                // Create 40 posts for each thread
                LocalDateTime lastPostTime = createdTime;
                for (int postNum = 0; postNum < 40; postNum++) {
                    User postAuthor = users.get(random.nextInt(users.size()));
                    String postContent = getRandomPostContent();
                    
                    Post post = new Post(thread, postAuthor, postContent);
                    // Posts are created after the thread, with some time gaps
                    lastPostTime = lastPostTime.plusMinutes(random.nextInt(60 * 24)); // Add 0-24 hours
                    post.setCreatedAt(lastPostTime);
                    
                    postRepository.save(post);
                    totalPosts++;
                }
                
                // Update thread's last post time
                thread.setLastPostAt(lastPostTime);
                threadRepository.save(thread);
            }
        }
        
        System.out.println("✅ Created " + totalThreads + " threads");
        System.out.println("✅ Created " + totalPosts + " posts");
    }

    private String getRandomThreadTitle() {
        return threadTitles[random.nextInt(threadTitles.length)];
    }

    private String getRandomPostContent() {
        // Sometimes combine multiple sentences for longer posts
        if (random.nextDouble() < 0.3) {
            return postContents[random.nextInt(postContents.length)] + "\n\n" +
                   postContents[random.nextInt(postContents.length)];
        }
        return postContents[random.nextInt(postContents.length)];
    }
}