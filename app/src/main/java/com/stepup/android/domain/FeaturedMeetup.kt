package com.stepup.android.domain

/** A real, future, public meetup with space. No fabricated fallback or location claims. */
fun featuredMeetup(posts: List<Post>, nowMillis: Long): Post? = posts
    .filter { it.isFlash && it.crewId.isBlank() && it.meetAt > nowMillis && !it.isFull }
    .minWithOrNull(compareBy<Post> { it.meetAt }.thenBy { it.id })
