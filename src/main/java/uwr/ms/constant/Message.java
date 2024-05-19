package uwr.ms.constant;

public enum Message {
    //permissions
    EDIT_TRIP_PERMISSION_DENIED("You do not have permission to edit this trip."),
    PERMISSION_DENIED_INVITE("You do not have permission to invite participants to this trip."),
    PERMISSION_DENIED_REMOVE_PARTICIPANT("You do not have permission to remove participants from this trip."),
    //passwords
    CURRENT_PASSWORD_INCORRECT("Current password is incorrect."),
    INVALID_OLD_PASSWORD("Invalid old password."),
    PASSWORDS_DONT_MATCH("New password and confirmed new password don't match."),
    PASSWORD_CHANGE_FAILED("An error occurred while changing the password: %s"),
    PASSWORD_CHANGE_SUCCESS("Password successfully changed."),
    //trip invitations
    ERROR_ACCEPTING_INVITATION("Error accepting invitation: %s"),
    ERROR_DECLINING_INVITATION("Error declining invitation: %s"),
    INVITATION_ACCEPTED_SUCCESS("Invitation accepted successfully!"),
    INVITATION_ALREADY_SENT("An invitation has already been sent to this user."),
    INVITATION_DECLINED_SUCCESS("Invitation declined successfully!"),
    INVITATION_NOT_FOUND("Invitation not found."),
    INVITING_USER_NOT_FOUND("Inviting user not found."),
    USER_INVITED_SUCCESS("User invited successfully!"),
    USER_TO_INVITE_NOT_FOUND("User to invite not found."),
    ACCESS_DENIED_TRIP_INVITATION("You do not have permission to edit this trip invitation."),
    //events
    EVENT_DELETED_SUCCESS("Event deleted successfully!"),
    EVENT_NOT_BELONG_TO_TRIP("Event does not belong to the specified trip."),
    EVENT_NOT_FOUND("Event not found."),
    EVENT_SAVED_SUCCESS("Event saved successfully!"),
    INVALID_DATE_TIME("Invalid date and time."),
    INVALID_LOCATION("Invalid location."),
    INVALID_ORIGIN_DESTINATION("Invalid origin and destination."),
    //expenses
    EXPENSE_ADDED_SUCCESS("Expense added successfully!"),
    EXPENSE_DELETED_SUCCESSFULLY("Expense deleted successfully!"),
    EXPENSE_DELETE_FAILED("Failed to delete expense: %s"),
    PAYER_NOT_FOUND("Payer not found"),
    //friend requests
    FRIEND_REQUEST_ACCEPTED("Friend request accepted successfully!"),
    FRIEND_REQUEST_ACCEPT_FAILED("Failed to accept friend request: %s"),
    ACCESS_DENIED_ACCEPT_FRIEND_REQUEST("You do not have permission to accept this friend request."),
    FRIEND_REQUEST_BLOCKED("Friend request blocked."),
    FRIEND_REQUEST_BLOCK_FAILED("Failed to block friend request: %s"),
    ACCESS_DENIED_BLOCK_FRIEND_REQUEST("You do not have permission to block this friend request."),
    ACCESS_DENIED_UNBLOCK_FRIEND_REQUEST("You do not have permission to unblock this friend request."),
    FRIEND_REQUEST_DECLINED("Friend request declined."),
    ACCESS_DENIED_DECLINE_FRIEND_REQUEST("You do not have permission to decline this friend request."),
    FRIEND_REQUEST_DECLINE_FAILED("Failed to decline friend request: "),
    FRIEND_REQUEST_FAILED("Failed to send friend request: %s"),
    FRIEND_REQUEST_SENT_SUCCESS("Friend request sent successfully to %s!"),
    UNFRIEND_FAILED("Failed to unfriend %s!"),
    UNFRIEND_SUCCESS("Successfully unfriended %s!"),
    USER_UNBLOCKED_SUCCESS("User unblocked successfully."),
    USER_UNBLOCK_FAILED("Failed to unblock user: %s"),
    REQUESTER_NOT_FOUND("Requester not found."),
    ADDRESSEE_NOT_FOUND("Addressee not found."),
    CANNOT_FRIEND_YOURSELF("Can not send friend request to yourself."),
    ALREADY_FRIENDS("You are already friends."),
    BLOCKED_BY_YOU("You blocked user %s."),
    BLOCKED_BY_USER("User %s blocked your friend requests."),
    REQUEST_ALREADY_SENT("Friend request already sent to %s."),
    REQUEST_ALREADY_RECEIVED("User %s already sent friend request to you."),
    FRIEND_REQUEST_NOT_FOUND("Friend request not found."),
    //trips
    PARTICIPANT_ALREADY_ADDED("Participant is already added to this trip."),
    PARTICIPANT_NOT_ASSOCIATED("Participant is not associated with this trip."),
    PARTICIPANT_NOT_FOUND("Participant not found."),
    PARTICIPANT_REMOVED_SUCCESS("Participant removed successfully."),
    INVALID_TRIP_ID("Invalid trip Id: %s."),
    TRIP_NAME_EXISTS("You already have a trip with the same name."),
    TRIP_NOT_FOUND("Trip not found."),
    TRIP_NOT_PARTICIPANT("You are not a participant of this trip."),
    TRIP_UPDATED_SUCCESS("Trip updated successfully!"),
    USER_ALREADY_PARTICIPANT("User is already a participant of the trip."),
    //users
    IMAGE_URL_TOO_LONG("Image URL is longer than 255 characters."),
    NAME_TOO_LONG("Name is longer than 39 characters."),
    NAME_CANNOT_BE_EMPTY("Name can not be empty."),
    INVALID_USER_INSTANCE("User must be an instance of AppUser."),
    PROFILE_UPDATE_SUCCESS("Profile updated successfully."),
    PROFILE_UPDATE_FAILED("Failed to update profile: %s"),
    REGISTRATION_SUCCESS("Registration successful, you can now log in."),
    USER_ALREADY_EXISTS("User %s already exists."),
    USER_NOT_FOUND("User %s not found.");

    private final String message;

    Message(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}