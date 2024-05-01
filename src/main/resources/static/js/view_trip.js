let mapPreview;
let currentEventIndex = 0;
let slideshowInterval = null;

function loadGoogleMapsScript(callback) {
    var map = document.getElementById('mapContainer');
    var apiKey = map.getAttribute('data-api-key');
    const script = document.createElement('script');
    script.src = 'https://maps.googleapis.com/maps/api/js?key=' + apiKey + '&loading=async' + '&libraries=places' + '&callback=initializeFirstEvent';
    script.id = 'googleMapsScript';
    document.head.appendChild(script);
    console.log('ending load map script')
}

function initializeFirstEvent() {
    const firstEventLink = document.querySelector('.event-link');
    if (firstEventLink) {
        firstEventLink.classList.add('selected');
        showEventOnMap(firstEventLink);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    loadGoogleMapsScript();

    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const activeTabs = document.querySelectorAll('.nav-link.active, .tab-pane.active');
            activeTabs.forEach(tab => tab.classList.remove('active', 'show'));

            this.classList.add('active');
            const activePane = document.querySelector(this.getAttribute('href'));
            activePane.classList.add('show', 'active');
        });
    });

    document.querySelectorAll('.event-link').forEach((link, index) => {
        link.addEventListener('click', function(event) {
            event.preventDefault();
            currentEventIndex = index;
            showEventOnMap(this);
            highlightCurrentEvent(this);
            if (slideshowInterval) {
                clearInterval(slideshowInterval);
                startSlideshow();
            }
        });
    });

    const startButton = document.getElementById('startSlideshowBtn');
    const pauseButton = document.getElementById('pauseSlideshowBtn');
    const speedSlider = document.getElementById('slideshowSpeed');
    const speedDisplay = document.getElementById('speedDisplay');
    startButton.addEventListener('click', () => {
        startSlideshow();
        pauseButton.disabled = false;
        startButton.disabled = true;
    });

    pauseButton.addEventListener('click', () => {
        pauseSlideshow();
        pauseButton.disabled = true;
        startButton.disabled = false;
    });

    speedSlider.addEventListener('input', () => {
        speedDisplay.textContent = speedSlider.value + ' sec';
        if (slideshowInterval) {
            clearInterval(slideshowInterval);
            startSlideshow();
        }
    });
});

function showEventOnMap(anchor) {

    const eventName = anchor.getAttribute('data-event-name');
    const eventType = anchor.getAttribute('data-event-type');
    const location = anchor.getAttribute('data-location');
    const origin = anchor.getAttribute('data-origin');
    const destination = anchor.getAttribute('data-destination');
    const travelMode = anchor.getAttribute('data-travel-mode');
    const zoomLevel = parseInt(anchor.getAttribute('data-zoom'), 10);

    console.log('Event Details:', eventName, eventType, location, origin, destination, travelMode, zoomLevel);

    const mapContainer = document.getElementById('mapContainer');
    mapPreview = new google.maps.Map(mapContainer, {
        center: { lat: 0, lng: 0 },
        zoom: zoomLevel,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    });

    if (eventType === "ROUTE" && origin && destination) {
        const directionsService = new google.maps.DirectionsService();
        const directionsRenderer = new google.maps.DirectionsRenderer({ map: mapPreview });

        directionsService.route({
            origin: origin,
            destination: destination,
            travelMode: google.maps.TravelMode[travelMode.toUpperCase()]
        }, function(response, status) {
            if (status === 'OK') {
                directionsRenderer.setDirections(response);
            } else {
                console.error('Failed to load directions:', status);
            }
        });
    } else if (eventType === "SINGLE" && location) {
        const service = new google.maps.places.PlacesService(mapPreview);
        service.textSearch({ query: location }, function(results, status) {
            if (status === google.maps.places.PlacesServiceStatus.OK) {
                const place = results[0];
                mapPreview.setCenter(place.geometry.location);
                new google.maps.Marker({
                    position: place.geometry.location,
                    map: mapPreview,
                    title: eventName
                });
            } else {
                console.error('Place not found:', status);
            }
        });
    }
}

function sendFriendRequest(username, event) {
    const button = event.target;

    const url = `/friends/add-friend`;
    const params = new URLSearchParams();
    params.append('username', username);
    const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');

    fetch(url, {
        method: 'POST',
        body: params,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': csrfToken
        }
    }).then(response => {
        if (response.ok) {
            alert('Friend request sent!');
            button.textContent = 'Request Sent';
            button.disabled = true;
            button.classList.add('btn-success');
            button.classList.remove('btn-primary');
        } else {
            response.text().then(text => alert('Failed to send friend request: ' + text));
        }
    }).catch(error => {
        console.error('Error sending friend request:', error);
        alert('Error sending friend request: ' + error.message);
    });
}


function startSlideshow() {
    const interval = document.getElementById('slideshowSpeed').value * 1000;
    slideshowInterval = setInterval(() => {
        showNextEvent();
    }, interval);
}

function showNextEvent() {
    const events = document.querySelectorAll('.event-link');
    currentEventIndex++;
    if (currentEventIndex >= events.length) {
        currentEventIndex = 0;
    }
    showEventOnMap(events[currentEventIndex]);
    highlightCurrentEvent(events[currentEventIndex]);
}

function pauseSlideshow() {
    clearInterval(slideshowInterval);
    slideshowInterval = null;
}

function highlightCurrentEvent(eventElement) {
    document.querySelectorAll('.event-link').forEach(el => {
        el.classList.remove('selected');
    });
    eventElement.classList.add('selected');
}