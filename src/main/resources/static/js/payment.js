/* ═══════════════════════════════════════════════════════════
   BALAJI PHOTO FRAMES — payment.js
   Razorpay Payment Gateway Integration
   ═══════════════════════════════════════════════════════════ */

// ── Open Razorpay Checkout ────────────────────────────────────────────────────
function initiateRazorpayPayment() {
    const name    = document.getElementById('cName').value.trim();
    const phone   = document.getElementById('cPhone').value.trim();
    const email   = document.getElementById('cEmail').value.trim();
    const address = document.getElementById('cAddress').value.trim();

    // Validate
    if (!name)  { showToast('Please enter your name', true); return; }
    if (!phone || phone.replace(/\D/g,'').length < 10) {
        showToast('Please enter a valid 10-digit phone number', true); return;
    }
    if (!state.sizeId || !state.beadingId || !state.coverId) {
        showToast('Please select frame size, beading and cover', true); return;
    }

    const btn = document.getElementById('rzpPayBtn');
    btn.textContent = 'Creating order...';
    btn.disabled    = true;

    // Step 1: Create Razorpay order on backend
    fetch('/api/payment/create-order', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            frameSizeId:     state.sizeId,
            beadingOptionId: state.beadingId,
            coverOptionId:   state.coverId,
            customerName:    name,
            customerPhone:   phone,
            customerEmail:   email,
            deliveryAddress: address,
            uploadedPhotoPath: state.photoPath,
        }),
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            showToast(data.error, true);
            btn.textContent = '💳 Pay Now';
            btn.disabled    = false;
            return;
        }

        // Step 2: Open Razorpay checkout modal
        const options = {
            key:         data.razorpayKeyId,
            amount:      data.amountInPaise,
            currency:    data.currency,
            name:        'BALAJI Photo Frames',
            description: 'Custom Photo Frame Order',
            image:       '/images/logo.png',
            order_id:    data.razorpayOrderId,

            // Pre-fill customer details
            prefill: {
                name:    data.customerName,
                email:   data.customerEmail || '',
                contact: data.customerPhone,
            },

            notes: {
                address: 'Rajpur, Shamshabad Road, Agra 282001',
            },

            theme: {
                color: '#C9A84C',
                hide_topbar: false,
            },

            // ── Payment Success Callback ──────────────────────────────────
            handler: function (rzpResponse) {
                verifyPayment(
                    rzpResponse.razorpay_order_id,
                    rzpResponse.razorpay_payment_id,
                    rzpResponse.razorpay_signature,
                    data.internalOrderId
                );
            },

            // ── Modal Dismiss ─────────────────────────────────────────────
            modal: {
                ondismiss: function () {
                    showToast('Payment cancelled. You can try again.', true);
                    btn.textContent = '💳 Pay Now';
                    btn.disabled    = false;
                },
            },
        };

        const rzp = new Razorpay(options);

        // Handle payment failure inside checkout
        rzp.on('payment.failed', function (resp) {
            showToast('Payment failed: ' + resp.error.description, true);
            btn.textContent = '💳 Pay Now';
            btn.disabled    = false;
        });

        rzp.open();
    })
    .catch(err => {
        showToast('Network error. Please try again.', true);
        btn.textContent = '💳 Pay Now';
        btn.disabled    = false;
        console.error(err);
    });
}

// ── Step 3: Verify Payment on Backend ────────────────────────────────────────
function verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature, internalOrderId) {
    showToast('Verifying payment...');

    fetch('/api/payment/verify', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            razorpayOrderId,
            razorpayPaymentId,
            razorpaySignature,
            internalOrderId,
        }),
    })
    .then(r => r.json())
    .then(data => {
        if (data.success) {
            // Payment verified — show success UI
            showPaymentSuccess(data);
        } else {
            showToast('⚠️ ' + data.message, true);
            document.getElementById('rzpPayBtn').textContent = '💳 Pay Now';
            document.getElementById('rzpPayBtn').disabled    = false;
        }
    })
    .catch(() => {
        showToast('Verification error. Please contact us on WhatsApp.', true);
    });
}

// ── Show success screen ───────────────────────────────────────────────────────
function showPaymentSuccess(data) {
    document.getElementById('payFormStep').style.display  = 'none';
    document.getElementById('successStep').style.display  = 'block';
    document.getElementById('successOrderId').textContent = '#' + data.orderId;
    document.getElementById('successTotal').textContent   =
        document.getElementById('prTotal').textContent;
    document.getElementById('successName').textContent  =
        document.getElementById('cName').value.trim();
    document.getElementById('successPhone').textContent =
        document.getElementById('cPhone').value.trim();
    document.getElementById('waLink').href =
        `https://wa.me/918299576949?text=Hello%20BALAJI%20Frames!%20Payment%20done%20for%20Order%20%23${data.orderId}.%20Please%20confirm.`;
}
