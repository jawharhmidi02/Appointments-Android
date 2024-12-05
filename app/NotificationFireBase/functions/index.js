const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.sendAppointmentReminders = functions.pubsub.schedule("every 24 hours").onRun(async (context) => {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);

    const formattedDate = tomorrow.toISOString().split('T')[0]; // YYYY-MM-DD format

    const appointmentsRef = db.collection("Appointment");
    const availabilitiesRef = db.collection("Availability");
    const usersRef = db.collection("User");

    const snapshot = await appointmentsRef.get();

    const promises = snapshot.docs.map(async (doc) => {
        const appointment = doc.data();

        // Get availability details
        const availabilityDoc = await availabilitiesRef.doc(appointment.availabilityID).get();
        const availability = availabilityDoc.data();

        if (availability.date === formattedDate) {
            // Get user details
            const patientDoc = await usersRef.doc(appointment.patientID).get();
            const patient = patientDoc.data();

            const payload = {
                notification: {
                    title: "Appointment Reminder",
                    body: `You have an appointment with Dr. ${appointment.doctorID} on ${availability.date} at ${availability.startHour}.`,
                },
            };

            if (patient.fcmToken) {
                return admin.messaging().sendToDevice(patient.fcmToken, payload);
            }
        }
        return null;
    });

    await Promise.all(promises);
    console.log("Reminders sent!");
    return null;
});
