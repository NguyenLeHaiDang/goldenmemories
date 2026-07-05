document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".button, .nav-cta, .site-nav a").forEach((element) => {
    element.addEventListener("mouseenter", () => element.classList.add("is-hovered"));
    element.addEventListener("mouseleave", () => element.classList.remove("is-hovered"));
  });
});
