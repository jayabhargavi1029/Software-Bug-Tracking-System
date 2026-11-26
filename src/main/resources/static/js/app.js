// BugTrack - small UI helpers (no framework needed)
document.addEventListener('DOMContentLoaded', function () {

    // 1) Mobile sidebar: hamburger button opens it, the dark backdrop closes it
    var sidebar = document.getElementById('sidebar');
    var backdrop = document.getElementById('backdrop');
    var toggle = document.getElementById('menuToggle');
    function setSidebar(open) {
        if (!sidebar || !backdrop) return;
        sidebar.classList.toggle('open', open);
        backdrop.classList.toggle('show', open);
    }
    if (toggle) toggle.addEventListener('click', function () { setSidebar(!sidebar.classList.contains('open')); });
    if (backdrop) backdrop.addEventListener('click', function () { setSidebar(false); });

    // 2) Delete confirmation modal: the clicked button tells the modal which URL to post to
    var modal = document.getElementById('confirmModal');
    if (modal) {
        modal.addEventListener('show.bs.modal', function (event) {
            var button = event.relatedTarget;
            if (!button) return;
            document.getElementById('confirmForm').setAttribute('action', button.getAttribute('data-action'));
            document.getElementById('confirmMessage').textContent = button.getAttribute('data-message') || 'Are you sure?';
        });
    }

    // 3) Success messages disappear after a few seconds
    document.querySelectorAll('.auto-dismiss').forEach(function (box) {
        setTimeout(function () {
            box.style.transition = 'opacity .5s';
            box.style.opacity = '0';
            setTimeout(function () { box.remove(); }, 500);
        }, 4500);
    });

    // 4) Prevent double submit of forms
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type=submit]');
            if (btn) setTimeout(function () { btn.disabled = true; }, 0);
        });
    });
});
