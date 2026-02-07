package com.ecom.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import java.util.List;

@Entity
@Table(name = "community_posts")
public class CommunityPost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private UserDtls user;

	@ManyToOne
	@JoinColumn(name = "pet_id", nullable = false)
	private Pet pet;

	@Column(length = 1000)
	private String description;

	@Column(name = "post_image")
	private String postImage; // เพิ่ม field สำหรับรูปโพสต์

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Transient
	private boolean likedByCurrentUser;

	@Transient
	private long likeCount;

	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PetComment> comments;
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PostLike> likes;

	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Notification> notifications;
	@PrePersist
	public void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	// Constructors
	public CommunityPost() {
	}

	public CommunityPost(UserDtls user, Pet pet, String description, String postImage) {
		this.user = user;
		this.pet = pet;
		this.description = description;
		this.postImage = postImage;
	}

	// Getters and Setters (เพิ่ม getter/setter สำหรับ postImage)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserDtls getUser() {
		return user;
	}

	public void setUser(UserDtls user) {
		this.user = user;
	}

	public Pet getPet() {
		return pet;
	}

	public void setPet(Pet pet) {
		this.pet = pet;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPostImage() {
		return postImage;
	}

	public void setPostImage(String postImage) {
		this.postImage = postImage;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isLikedByCurrentUser() {
		return likedByCurrentUser;
	}

	public void setLikedByCurrentUser(boolean likedByCurrentUser) {
		this.likedByCurrentUser = likedByCurrentUser;
	}

	public long getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(long likeCount) {
		this.likeCount = likeCount;
	}
}