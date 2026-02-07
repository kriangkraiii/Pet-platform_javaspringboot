package com.ecom.controller;

import com.ecom.model.CommunityPost;
import com.ecom.model.PetComment;
import com.ecom.model.Pet;
import com.ecom.model.UserDtls;
import com.ecom.model.Category;
import com.ecom.repository.CommunityPostRepository;
import com.ecom.repository.NotificationRepository;
import com.ecom.repository.PetCommentRepository;
import com.ecom.repository.PetLikeRepository;
import com.ecom.repository.PostLikeRepository;
import com.ecom.service.PetService;
import com.ecom.service.UserService;
import com.ecom.service.AdminLogService;
import com.ecom.service.CategoryService;
import com.ecom.service.CommunityPostService;
import com.ecom.service.FileService;
import com.ecom.service.CartService;
import com.ecom.util.BucketType;
import com.ecom.util.CommonUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminCommunityController {

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private PetCommentRepository petCommentRepository;

    @Autowired
    private PetLikeRepository petLikeRepository;

    @Autowired
    private PetService petService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminLogService adminLogService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CartService cartService;

    @Autowired 
    private PostLikeRepository postLikeRepository;

    @Autowired 
    private NotificationRepository notificationRepository;
    
    @Autowired 
    private CommunityPostService communityPostService;
    
    @Autowired
    private FileService fileService;

    @ModelAttribute
    public void getUserDetails(Principal p, Model m) {
        if (p != null) {
            String email = p.getName();
            UserDtls userDtls = userService.getUserByEmail(email);
            m.addAttribute("user", userDtls);
            
            if (userDtls != null) {
                Integer countCart = cartService.getCountCart(userDtls.getId());
                m.addAttribute("countCart", countCart != null ? countCart : 0);
            }
        }

        List<Category> allActiveCategory = categoryService.getAllActiveCategory();
        m.addAttribute("categorys", allActiveCategory);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/community")
    public String adminCommunityPage(Model model, Principal principal,
                                    @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommunityPost> page = communityPostRepository.findAll(pageable);
        
        UserDtls currentAdmin = null;
        if (principal != null) {
            currentAdmin = userService.getUserByEmail(principal.getName());
        }

        for (CommunityPost post : page.getContent()) {
            long likeCount = postLikeRepository.countByPost(post);
            boolean likedByCurrentAdmin = (currentAdmin != null) &&
                                           postLikeRepository.existsByUserAndPost(currentAdmin, post);
            
            post.setLikeCount(likeCount);
            post.setLikedByCurrentUser(likedByCurrentAdmin);
        }

        model.addAttribute("communityPosts", page.getContent());
        model.addAttribute("pageNo", page.getNumber());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalElements", page.getTotalElements());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("isFirst", page.isFirst());
        model.addAttribute("isLast", page.isLast());

        return "admin/community";
    }

    @PostMapping("/community/post/{postId}/delete")
    public String deletePost(@PathVariable Long postId, Principal principal,
                             HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            if (principal == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "You need to login to delete posts.");
                return "redirect:/admin/community";
            }

            CommunityPost post = communityPostRepository.findById(postId).orElse(null);
            if (post == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "Post not found.");
                return "redirect:/admin/community";
            }

            boolean ok = communityPostService.deletePostWithDependencies(postId);
            if (!ok) {
                redirectAttributes.addFlashAttribute("errorMsg", "Post not found or already deleted.");
                return "redirect:/admin/community";
            }

            UserDtls admin = userService.getUserByEmail(principal.getName());
            String ipAddress = getClientIpAddress(request);
            adminLogService.logAction(
                admin.getEmail(),
                admin.getName(),
                "DELETE_COMMUNITY_POST",
                "Deleted community post ID: " + postId + " by user: " + post.getUser().getEmail(),
                ipAddress
            );

            redirectAttributes.addFlashAttribute("succMsg", "Post deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete post: " + e.getMessage());
        }

        return "redirect:/admin/community";
    }

    @GetMapping("/community/post/{postId}/edit")
    public String showEditPostForm(@PathVariable Long postId, Model model, Principal principal,
                                  RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Access denied.");
            return "redirect:/admin/community";
        }

        CommunityPost post = communityPostRepository.findById(postId).orElse(null);
        if (post == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Post not found.");
            return "redirect:/admin/community";
        }

        // Filter pets to only show pets owned by the post creator
        List<Pet> userPets = petService.getAllPets().stream()
                .filter(pet -> pet.getOwner().getId().equals(post.getUser().getId()))
                .collect(Collectors.toList());
        
        model.addAttribute("post", post);
        model.addAttribute("allPets", userPets); // Only pets owned by post creator
        return "admin/edit_community_post";
    }

    
    @PostMapping("/community/post/{postId}/update")
    public String updatePost(
            @PathVariable Long postId,
            @RequestParam int petId,
            @RequestParam String description,
            @RequestParam(value = "postImage", required = false) MultipartFile postImage,
            Principal principal,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            if (principal == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "Access denied.");
                return "redirect:/admin/community";
            }

            UserDtls admin = userService.getUserByEmail(principal.getName());
            CommunityPost post = communityPostRepository.findById(postId).orElse(null);
            String imageUrl = commonUtil.getImageUrl(postImage, BucketType.PETPOST.getId());
            
            if (post == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "Post not found.");
                return "redirect:/admin/community";
            }

            Pet pet = petService.getPetById(petId);
            if (pet == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "Invalid pet selection.");
                return "redirect:/admin/community";
            }

            String oldPostImage = post.getPostImage();
            post.setPet(pet);
            post.setDescription(description);

            if (postImage != null && !postImage.isEmpty()) {
                File staticDir = new ClassPathResource("static").getFile();

                if (oldPostImage != null && !oldPostImage.equals("/upload/posts/default.jpg")) {
                    String oldRelPath = oldPostImage.startsWith("/") ? oldPostImage.substring(1) : oldPostImage;
                    File oldFile = new File(staticDir, oldRelPath);
                    try {
                        Files.deleteIfExists(oldFile.toPath());
                    } catch (Exception e) {
                        System.err.println("Failed to delete old image: " + e.getMessage());
                    }
                }

                File uploadDir = new File(staticDir, "upload" + File.separator + "posts");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                String fileName = UUID.randomUUID() + "_" + postImage.getOriginalFilename();
                Path dest = Paths.get(uploadDir.getAbsolutePath(), fileName);
                Files.copy(postImage.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

                post.setPostImage(imageUrl);
            }

            communityPostRepository.save(post);

            String ipAddress = getClientIpAddress(request);
            adminLogService.logAction(
                    admin.getEmail(),
                    admin.getName(),
                    "UPDATE_COMMUNITY_POST",
                    "Updated community post ID: " + postId + " of user: " + post.getUser().getEmail(),
                    ipAddress
            );

            redirectAttributes.addFlashAttribute("succMsg", "Post updated successfully!");
            fileService.uploadFileS3(postImage, BucketType.PETPOST.getId());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update post: " + e.getMessage());
        }

        return "redirect:/admin/community";
    }

    @GetMapping("/community/post/{postId}/comments")
    public String viewPostComments(@PathVariable Long postId, Model model, Principal principal,
                                  RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Access denied.");
            return "redirect:/admin/community";
        }

        CommunityPost post = communityPostRepository.findById(postId).orElse(null);
        if (post == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Post not found.");
            return "redirect:/admin/community";
        }

        List<PetComment> comments = petCommentRepository.findByPostId(postId);
        long commentCount = petCommentRepository.countByPostId(postId);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentCount", commentCount);

        return "admin/post_comments";
    }

    @PostMapping("/community/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId, Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            if (principal == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "Access denied.");
                return "redirect:/admin/community";
            }

            UserDtls admin = userService.getUserByEmail(principal.getName());
            PetComment comment = petCommentRepository.findById(commentId).orElse(null);

            if (comment == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "Comment not found.");
                return "redirect:/admin/community";
            }

            Long postId = comment.getPost() != null ? comment.getPost().getId() : null;

            String ipAddress = getClientIpAddress(request);
            adminLogService.logAction(admin.getEmail(), admin.getName(), "DELETE_COMMENT",
                    "Deleted comment ID: " + commentId + " by user: " + comment.getUser().getEmail(), ipAddress);

            petCommentRepository.delete(comment);
            redirectAttributes.addFlashAttribute("succMsg", "Comment deleted successfully.");

            if (postId != null) {
                return "redirect:/admin/community/post/" + postId + "/comments";
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete comment: " + e.getMessage());
        }

        return "redirect:/admin/community";
    }
}
