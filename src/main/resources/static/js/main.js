document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".button, .btn, .nav-cta, .site-nav a, .navbar__nav a").forEach((element) => {
    element.addEventListener("mouseenter", () => element.classList.add("is-hovered"));
    element.addEventListener("mouseleave", () => element.classList.remove("is-hovered"));
  });

  const revealItems = Array.from(document.querySelectorAll("[data-reveal]"));
  if (!revealItems.length) {
    return;
  }

  if (!("IntersectionObserver" in window)) {
    revealItems.forEach((element) => element.classList.add("is-visible"));
    return;
  }

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.16 });

  revealItems.forEach((element) => observer.observe(element));
});
