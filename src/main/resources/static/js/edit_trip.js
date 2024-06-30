let mapPreview;

function loadGoogleMapsScript() {
    var map = document.getElementById('mapPreview');
    var apiKey = map.getAttribute('data-api-key');
    const script = document.createElement('script');
    script.src = 'https://maps.googleapis.com/maps/api/js?key=' + apiKey + '&loading=async' + '&libraries=places';
    script.id = 'googleMapsScript';
    document.head.appendChild(script);
}

document.addEventListener('DOMContentLoaded', function () {
    loadGoogleMapsScript();

    document.getElementById('saveChangesButton').addEventListener('click', function() {
        document.getElementById('editTripForm').submit();
    });

    document.getElementById('eventFormSubmitBtn').addEventListener('click', function (event) {
        const form = document.getElementById('eventForm');

        if (form.checkValidity() === false) {
            event.preventDefault();
            event.stopPropagation();
            alert("Please fill all required fields: Event Name, Location (Single Place) or Origin & Destination (Route), Date, Time");
        } else {
            form.submit();
        }
    });

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

    if (friendInput) {
        friendInput.addEventListener('input', filterFriends);
    }

    if(friendsList) {
        friendsList.addEventListener('click', function (e) {
            const target = e.target;
            const listItem = target.closest('li');
            if (listItem) {
                selectFriend(listItem);
            }
        });
    }


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

function toggleRouteFields() {
    const eventType = document.querySelector('input[name="eventType"]:checked').value;
    const locationField = document.getElementById('location');
    const originField = document.getElementById('origin');
    const destinationField = document.getElementById('destination');

    if (eventType === 'SINGLE') {
        locationField.required = true;
        originField.required = false;
        destinationField.required = false;
        document.getElementById('routeFields').style.display = 'none';
        document.getElementById('locationField').style.display = 'block';
    } else if (eventType === 'ROUTE') {
        locationField.required = false;
        originField.required = true;
        destinationField.required = true;
        document.getElementById('routeFields').style.display = 'block';
        document.getElementById('locationField').style.display = 'none';
    }
}

function updateDisplayStates(showMap) {
    const initialText = document.getElementById('initialText');
    const mapPreviewContainer = document.getElementById('mapPreview');
    const zoomControl = document.getElementById('zoomControl');

    if (showMap) {
        initialText.style.display = 'none';
        mapPreviewContainer.style.display = 'block';
        zoomControl.disabled = false;
    } else {
        initialText.style.display = 'block';
        mapPreviewContainer.style.display = 'none';
        zoomControl.disabled = true;
    }
}

function loadMapPreview() {
    const location = document.getElementById('location').value;
    const origin = document.getElementById('origin').value;
    const destination = document.getElementById('destination').value;
    const travelMode = document.getElementById('travelMode').value;
    const mapPreviewContainer = document.getElementById('mapPreview');

    mapPreview = new google.maps.Map(mapPreviewContainer, {
        center: { lat: 0, lng: 0 },
        mapTypeId: google.maps.MapTypeId.ROADMAP
    });

    const eventType = document.querySelector('input[name="eventType"]:checked').value;

    if (eventType === "ROUTE" && origin && destination) {
        const directionsService = new google.maps.DirectionsService();
        const directionsRenderer = new google.maps.DirectionsRenderer({ map: mapPreview });

        directionsService.route({
            origin: origin,
            destination: destination,
            travelMode: google.maps.TravelMode[travelMode.toUpperCase()]
        }, (response, status) => {
            if (status === 'OK') {
                directionsRenderer.setDirections(response);
                updateDisplayStates(true);
            } else {
                alert('Directions request failed due to ' + status);
            }
        });
    } else if (eventType === "SINGLE" && location) {
        const service = new google.maps.places.PlacesService(mapPreview);
        service.textSearch({ query: location }, function(results, status) {
            if (status === google.maps.places.PlacesServiceStatus.OK) {
                const place = results[0];
                mapPreview.setCenter(place.geometry.location);

                var marker = new google.maps.Marker({
                    position: place.geometry.location,
                    map: mapPreview,
                    title: place.name
                });

                const zoomLevel = parseInt(document.getElementById('zoomControl').value);
                mapPreview.setZoom(zoomLevel);

                updateDisplayStates(true);
            } else {
                alert('Place not found: ' + status);
            }
        });
    }
}

function adjustZoom() {
    const zoomLevel = parseInt(document.getElementById('zoomControl').value);
    mapPreview.setZoom(zoomLevel);
}

