/* ═══════════════════════════════════════════════════════════
   BALAJI PHOTO FRAMES — app.js
   ═══════════════════════════════════════════════════════════ */

/* ── State ──────────────────────────────────────────────── */
const state = {
    sizeId:     null,
    beadingId:  null,
    coverId:    null,
    photoPath:  null,
    photoDataUrl: null,
};

/* ── DOM refs ───────────────────────────────────────────── */
const uploadZone   = document.getElementById('uploadZone');
const fileInput    = document.getElementById('fileInput');
const photoPreview = document.getElementById('photoPreview');
const frameCanvas  = document.getElementById('frameCanvas');
const frameOuter   = document.getElementById('frameOuter');
const frameBeading = document.getElementById('frameBeading');
const framePhoto   = document.getElementById('framePhotoArea');
const frameImg     = document.getElementById('framePhotoImg');
const framePlaceholder = document.getElementById('framePlaceholder');
const glassShimmer = document.getElementById('glassShimmer');
const prSize       = document.getElementById('prSize');
const prBeading    = document.getElementById('prBeading');
const prCover      = document.getElementById('prCover');
const prTotal      = document.getElementById('prTotal');
const modalBackdrop= document.getElementById('modalBackdrop');
const payFormStep  = document.getElementById('payFormStep');
const successStep  = document.getElementById('successStep');

/* ── Init ───────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
    initUploadZone();
    initPayTabs();
    // Auto-select first of each on page load
    const firstSize    = document.querySelector('.size-btn');
    const firstBeading = document.querySelector('.bead-card');
    const firstCover   = document.querySelector('.cover-card');
    if (firstSize)    firstSize.click();
    if (firstBeading) firstBeading.click();
    if (firstCover)   firstCover.click();
});

/* ── Upload Zone ────────────────────────────────────────── */
function initUploadZone() {
    // Click to browse
    uploadZone.addEventListener('click', () => fileInput.click());

    // Drag over
    uploadZone.addEventListener('dragover', e => {
        e.preventDefault();
        uploadZone.classList.add('dragging');
    });
    uploadZone.addEventListener('dragleave', () => {
        uploadZone.classList.remove('dragging');
    });

    // Drop
    uploadZone.addEventListener('drop', e => {
        e.preventDefault();
        uploadZone.classList.remove('dragging');
        const file = e.dataTransfer.files[0];
        if (file && file.type.startsWith('image/')) {
            processFile(file);
        }
    });

    // Input change
    fileInput.addEventListener('change', () => {
        if (fileInput.files[0]) processFile(fileInput.files[0]);
    });
}

function processFile(file) {
    const reader = new FileReader();
    reader.onload = e => {
        state.photoDataUrl = e.target.result;

        // Show preview in upload zone
        photoPreview.src = e.target.result;
        photoPreview.style.display = 'block';
        uploadZone.querySelector('.upload-icon').style.display = 'none';
        uploadZone.querySelector('.upload-text').style.display = 'none';
        uploadZone.classList.add('has-photo');

        // Show in frame
        frameImg.src = e.target.result;
        frameImg.style.display = 'block';
        framePlaceholder.style.display = 'none';

        // Upload to server
        uploadToServer(file);
    };
    reader.readAsDataURL(file);
}

function uploadToServer(file) {
    const fd = new FormData();
    fd.append('file', file);
    fetch('/api/frame/upload', { method: 'POST', body: fd })
        .then(r => r.json())
        .then(data => {
            if (data.path) {
                state.photoPath = data.path;
            }
        })
        .catch(() => {/* silent - still works with local preview */});
}

/* ── Size Selection ─────────────────────────────────────── */
function selectSize(el, id) {
    document.querySelectorAll('.size-btn').forEach(b => b.classList.remove('active'));
    el.classList.add('active');
    state.sizeId = id;
    fetchPrice();
}

/* ── Beading Selection ──────────────────────────────────── */
function selectBeading(el, id, borderPx, gradient) {
    document.querySelectorAll('.bead-card').forEach(b => b.classList.remove('active'));
    el.classList.add('active');
    state.beadingId = id;
    // Update preview border
    updateFramePreview(borderPx, gradient);
    fetchPrice();
}

/* ── Cover Selection ────────────────────────────────────── */
function selectCover(el, id, coverType) {
    document.querySelectorAll('.cover-card').forEach(b => b.classList.remove('active'));
    el.classList.add('active');
    state.coverId = id;
    // Show/hide glass shimmer
    glassShimmer.style.display =
        (coverType === 'GLASS' || coverType === 'ACRYLIC') ? 'block' : 'none';
    fetchPrice();
}

/* ── Frame Preview ──────────────────────────────────────── */
function updateFramePreview(borderPx, gradient) {
    const sizeBtn = document.querySelector('.size-btn.active');
    if (!sizeBtn) return;

    const w = parseInt(sizeBtn.dataset.w || 8);
    const h = parseInt(sizeBtn.dataset.h || 10);
    const maxDim = 200;
    const ratio  = w / h;

    let pw, ph;
    if (ratio >= 1) { pw = maxDim; ph = Math.round(maxDim / ratio); }
    else            { ph = maxDim; pw = Math.round(maxDim * ratio); }

    const bp = parseInt(borderPx || 10);
    const totalW = pw + bp * 2;
    const totalH = ph + bp * 2;

    frameOuter.style.width  = totalW + 'px';
    frameOuter.style.height = totalH + 'px';
    frameBeading.style.background = gradient || 'linear-gradient(135deg,#C8A96E,#8B6914)';
    framePhoto.style.width  = pw + 'px';
    framePhoto.style.height = ph + 'px';
    framePhoto.style.top    = bp + 'px';
    framePhoto.style.left   = bp + 'px';
}

/* ── Price Fetch ────────────────────────────────────────── */
function fetchPrice() {
    if (!state.sizeId || !state.beadingId || !state.coverId) return;

    fetch(`/api/frame/price?sizeId=${state.sizeId}&beadingId=${state.beadingId}&coverId=${state.coverId}`)
        .then(r => r.json())
        .then(data => {
            prSize.textContent    = `${data.sizeLabel} — ₹${fmt(data.basePrice)}`;
            prBeading.textContent = `₹${fmt(data.beadingPrice)}`;
            prCover.textContent   = `₹${fmt(data.coverPrice)}`;
            prTotal.textContent   = `₹ ${fmt(data.totalPrice)}`;
            document.getElementById('payTotalBtn').textContent = `₹${fmt(data.totalPrice)}`;
            const codBtn2 = document.getElementById('codTotalBtn');
            if (codBtn2) codBtn2.textContent = `₹${fmt(data.totalPrice)}`;
        })
        .catch(() => {});

    // Also update frame preview dimensions
    const activeBeadCard = document.querySelector('.bead-card.active');
    if (activeBeadCard) {
        updateFramePreview(activeBeadCard.dataset.borderPx, activeBeadCard.dataset.gradient);
    }
}

function fmt(num) {
    return parseFloat(num).toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

/* ── Payment Modal ──────────────────────────────────────── */
function openPayment() {
    if (!state.sizeId || !state.beadingId || !state.coverId) {
        showToast('Please select size, beading and cover first!', true);
        return;
    }
    payFormStep.style.display = 'block';
    successStep.style.display = 'none';
    modalBackdrop.classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closePayment() {
    modalBackdrop.classList.remove('open');
    document.body.style.overflow = '';
}

// Close on backdrop click
modalBackdrop.addEventListener('click', e => {
    if (e.target === modalBackdrop) closePayment();
});

/* ── Payment Tabs ───────────────────────────────────────── */
function initPayTabs() {
    document.querySelectorAll('.pay-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const target = tab.dataset.tab;
            document.querySelectorAll('.pay-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.pay-pane').forEach(p => p.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById('pane-' + target).classList.add('active');

            // Show Razorpay button for UPI + Card, COD button for Cash
            const rzpBtn  = document.getElementById('rzpPayBtn');
            const codBtn  = document.getElementById('codConfirmBtn');
            if (rzpBtn && codBtn) {
                if (target === 'cash') {
                    rzpBtn.style.display = 'none';
                    codBtn.style.display = 'block';
                } else {
                    rzpBtn.style.display = 'block';
                    codBtn.style.display = 'none';
                }
            }
        });
    });
}

/* ── Card Number Formatting ─────────────────────────────── */
function formatCard(input) {
    let v = input.value.replace(/\D/g, '').substring(0, 16);
    input.value = v.replace(/(.{4})/g, '$1 ').trim();
}

/* ── Submit Order ───────────────────────────────────────── */
function submitOrder() {
    const name    = document.getElementById('cName').value.trim();
    const phone   = document.getElementById('cPhone').value.trim();
    const email   = document.getElementById('cEmail').value.trim();
    const address = document.getElementById('cAddress').value.trim();
    const payMethod = document.querySelector('.pay-tab.active')?.dataset.tab?.toUpperCase() || 'CASH';

    // Validation
    if (!name)  { showToast('Please enter your name', true); return; }
    if (!phone || phone.length < 10) { showToast('Please enter a valid phone number', true); return; }

    const payload = {
        customerName:    name,
        customerPhone:   phone,
        customerEmail:   email,
        deliveryAddress: address,
        frameSizeId:     state.sizeId,
        beadingOptionId: state.beadingId,
        coverOptionId:   state.coverId,
        paymentMethod:   payMethod,
        uploadedPhotoPath: state.photoPath,
    };

    const btn = document.getElementById('submitOrderBtn');
    btn.textContent = 'Placing order...';
    btn.disabled    = true;

    fetch('/api/order/place', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(payload),
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) { showToast(data.error, true); btn.textContent = 'Confirm Order'; btn.disabled = false; return; }

        // Show success
        payFormStep.style.display = 'none';
        successStep.style.display  = 'block';
        document.getElementById('successName').textContent  = name;
        document.getElementById('successPhone').textContent = phone;
        document.getElementById('successOrderId').textContent = '#' + data.orderId;
        document.getElementById('successTotal').textContent   = '₹' + fmt(data.totalPrice);
        document.getElementById('waLink').href =
            `https://wa.me/918299576949?text=Hello%20BALAJI%20Frames!%20My%20order%20%23${data.orderId}%20has%20been%20placed.%20Name:%20${encodeURIComponent(name)}%20Phone:%20${phone}`;
    })
    .catch(() => {
        showToast('Network error. Please try again.', true);
        btn.textContent = 'Confirm Order';
        btn.disabled    = false;
    });
}

/* ── Toast ──────────────────────────────────────────────── */
function showToast(msg, isError = false) {
    const t = document.getElementById('toast');
    t.textContent = msg;
    t.className   = 'toast' + (isError ? ' toast-error' : '');
    t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 3500);
}
