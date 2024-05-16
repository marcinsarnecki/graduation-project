document.addEventListener('DOMContentLoaded', function () {
    const amountInput = document.getElementById('amount');
    const currencySelector = document.getElementById('currencySelector');
    const toggleSplitMode = document.getElementById('toggleSplitMode');
    const participantContainer = document.getElementById('participantsContainer');
    let isAdvanced = false;



    toggleSplitMode.addEventListener('click', () => {
        isAdvanced = !isAdvanced;
        toggleSplitMode.textContent = isAdvanced ? 'Simple mode' : 'Advanced mode';

        participantContainer.querySelectorAll('.weight-input').forEach((input, index) => {
            const checkbox = participantContainer.querySelectorAll('.participant-checkbox')[index];
            input.style.display = isAdvanced ? 'inline-block' : 'none';
            if (checkbox.checked) {
                input.value = '1';
            } else {
                input.value = '0';
            }
        });
        calculateShares();
    });

    participantContainer.addEventListener('input', event => {
        if (event.target.matches('.participant-checkbox')) {
            const weightInput = event.target.closest('.participant-row').querySelector('.weight-input');
            if (isAdvanced) {

                if (event.target.checked) {
                    weightInput.value = '1';
                }
                else {
                    weightInput.value = '0';
                }
            }
            calculateShares();
        } else if (event.target.matches('.weight-input')) {
            const checkbox = event.target.closest('.participant-row').querySelector('.participant-checkbox');
            const weight = parseFloat(event.target.value);
            if (weight > 0 && !checkbox.checked) {
                checkbox.checked = true;
            } else if (weight === 0) {
                checkbox.checked = false;
            }
            calculateShares();
        }
    });

    currencySelector.addEventListener('change', calculateShares);
    amountInput.addEventListener('input', calculateShares);

    calculateShares();

    document.querySelectorAll('.nav-link').forEach(function (link) {
        link.addEventListener('click', switchTab);
    });

    const form = document.querySelector('#expenseForm');
    form.addEventListener('submit', function(event) {
        participantContainer.querySelectorAll('.share-input').forEach((input, index) => {
            let cleanedValue = parseFloat(input.value.replace(/[^\d.-]/g, ''));
            cleanedValue = Math.round(cleanedValue * 100);

            if (cleanedValue !== 0) {
                const hiddenInput = document.createElement('input');
                hiddenInput.type = 'hidden';
                hiddenInput.name = 'participantAmounts';
                hiddenInput.value = cleanedValue;
                form.appendChild(hiddenInput);
            }
        });
        let cleanedValue = parseFloat(amountInput.value.replace(/[^\d.-]/g, ''));
        cleanedValue = Math.round(cleanedValue * 100);
        amountInput.value = cleanedValue;
    });

    function switchTab(event) {
        event.preventDefault();
        document.querySelectorAll('.nav-link').forEach(function (link) {
            link.classList.remove('active');
        });
        document.querySelectorAll('.tab-pane').forEach(function (pane) {
            pane.classList.remove('show', 'active');
        });

        this.classList.add('active');
        const activePaneId = this.getAttribute('href');
        const activePane = document.querySelector(activePaneId);
        if (activePane) {
            activePane.classList.add('show', 'active');
        }
    }
});

function toggleExpenseDetail(index) {
    const detailElement = document.querySelector('#detail-' + index);
    const allDetails = document.querySelectorAll('.expense-detail');
    allDetails.forEach(element => {
        if (element === detailElement) {
            element.style.display = element.style.display === 'none' ? 'block' : 'none';
        } else {
            element.style.display = 'none';
        }
    });
}

function toggleTransferDetail(index) {
    const detailElement = document.querySelector('#transfer-detail-' + index);
    const allDetails = document.querySelectorAll('.transfer-detail');
    allDetails.forEach(element => {
        if (element === detailElement) {
            element.style.display = element.style.display === 'none' ? 'block' : 'none';
        } else {
            element.style.display = 'none';
        }
    });
}

function openAddExpenseForm(button) {
    const debtorUsername = button.getAttribute('data-debtor-username');
    const creditorUsername = button.getAttribute('data-creditor-username');
    const amount = button.getAttribute('data-amount');

    const titleInput = document.getElementById('title');
    const amountInput = document.getElementById('amount');
    const payerSelect = document.getElementById('payer');
    const participantCheckboxes = document.querySelectorAll('.participant-checkbox');
    const dateInput = document.getElementById('date');

    titleInput.value = 'Return for ' + creditorUsername;
    amountInput.value = (amount / 100).toFixed(2);
    payerSelect.value = debtorUsername;

    participantCheckboxes.forEach(checkbox => {
        checkbox.checked = checkbox.value === creditorUsername;
    });

    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    dateInput.value = `${year}-${month}-${day}`;

    const addExpenseTabLink = document.querySelector('a[href="#addExpense"]');
    activateTab(addExpenseTabLink);
    calculateShares();
}

function activateTab(tabLink) {
    document.querySelectorAll('.nav-link').forEach(function (link) {
        link.classList.remove('active');
    });
    document.querySelectorAll('.tab-pane').forEach(function (pane) {
        pane.classList.remove('show', 'active');
    });

    tabLink.classList.add('active');
    const activePaneId = tabLink.getAttribute('href');
    const activePane = document.querySelector(activePaneId);
    if (activePane) {
        activePane.classList.add('show', 'active');
    }
}

function calculateShares() {
    const amountInput = document.getElementById('amount');
    const currencySelector = document.getElementById('currencySelector');
    const participantContainer = document.getElementById('participantsContainer');
    const totalAmount = parseFloat(amountInput.value) || 0;
    const currency = currencySelector.value;
    const participants = Array.from(participantContainer.querySelectorAll('.participant-checkbox'));
    const weights = participantContainer.querySelectorAll('.weight-input');
    const shares = participantContainer.querySelectorAll('.share-input');
    let isAdvanced = document.getElementById('toggleSplitMode').textContent === 'Simple mode';

    if (isAdvanced) {
        let totalWeight = 0;
        participants.forEach((participant, index) => {
            const weight = Math.max(parseFloat(weights[index].value) || 0, 0);
            weights[index].value = weight;
            if (participant.checked) {
                totalWeight += weight;
            }
            weights[index].style.display = 'inline-block';
        });

        participants.forEach((participant, index) => {
            if (participant.checked) {
                const weight = parseFloat(weights[index].value) || 0;
                const share = totalWeight > 0 ? ((totalAmount * weight) / totalWeight).toFixed(2) : 0;
                shares[index].value = `${share} ${currency}`;
            } else {
                shares[index].value = `0.00 ${currency}`;
            }
        });
    } else {
        const selectedParticipants = participants.filter(participant => participant.checked);
        const share = (totalAmount / selectedParticipants.length).toFixed(2);

        participants.forEach((participant, index) => {
            weights[index].style.display = 'none';
            shares[index].value = participant.checked ? `${share} ${currency}` : `0.00 ${currency}`;
        });
    }
}


