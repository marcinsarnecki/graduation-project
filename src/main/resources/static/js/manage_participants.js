document.addEventListener('DOMContentLoaded', function () {
    const friendInput = document.getElementById("friendInput");
    const friendsList = document.getElementById("friendsList");
    const addBtn = document.querySelector('.btn-primary');

    function clearStyles() {
        document.querySelectorAll('.list-group-item').forEach(el => {
            el.classList.remove('selected', 'first-visible', 'last-visible');
            el.style.backgroundColor = "";
        });
    }

    function filterFriends() {
        addBtn.disabled = true;
        const filter = friendInput.value.toUpperCase();
        const li = friendsList.getElementsByTagName('li');

        clearStyles();

        let visibleItems = [];
        Array.from(li).forEach(item => {
            const txtValue = item.textContent || item.innerText;
            if (txtValue.toUpperCase().indexOf(filter) > -1) {
                item.style.display = "";
                visibleItems.push(item);
            } else {
                item.style.display = "none";
            }
        });

        if (visibleItems.length > 0) {
            visibleItems[0].classList.add('first-visible');
            visibleItems[visibleItems.length - 1].classList.add('last-visible');
        }
    }

    function selectFriend(selectedItem) {
        friendInput.value = selectedItem.textContent.split(' (')[0].trim();
        addBtn.disabled = false;

        clearStyles();

        selectedItem.classList.add('selected');
        selectedItem.style.backgroundColor = "#dff0d8";

        const visibleItems = Array.from(document.querySelectorAll('#friendsList li'))
            .filter(li => li.style.display !== 'none');

        if (visibleItems.length > 0) {
            visibleItems[0].classList.add('first-visible');
            visibleItems[visibleItems.length - 1].classList.add('last-visible');
        }
    }

    friendInput.addEventListener('input', filterFriends);

    friendsList.addEventListener('click', function(e) {
        if (e.target.tagName === 'LI') {
            selectFriend(e.target);
        }
    });

    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('show', 'active'));

            this.classList.add('active');
            const activeTab = document.querySelector(this.getAttribute('href'));
            activeTab.classList.add('show', 'active');
        });
    });
});
