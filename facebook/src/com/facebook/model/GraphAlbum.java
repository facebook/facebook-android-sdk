package com.facebook.model;

/**
 * Provides a strongly-typed representation of the Album as defined by the Graph
 * API. See http://developers.facebook.com/docs/reference/api/album/ for details.
 *
 * Note that this interface is intended to be used with GraphObject.Factory and
 * not implemented directly.
 *
 * @author ogunwale
 *
 */
public interface GraphAlbum extends GraphObject {

	/**
     * Returns the ID of the album.
     * @return the ID of the album
     */
    public String getId();

    /**
     * Sets the ID of the album.
     * @param Id the ID of the album
     */
    public void setId(String Id);

    /**
     * Returns the name of the album.
     * @return the name of the album
     */
    public String getName();

    /**
     * Sets the name of the album.
     * @param name the name of the album
     */
    public void setName(String name);

    /**
     * Returns the description of the album.
     * @return the description of the album
     */
    public String getDescription();

    /**
     * Sets the description of the album.
     * @param description the description of the album
     */
    public void setDescription(String description);

    /**
     * Returns the location of the album.
     * @return the location of the album
     */
    public String getLocation();

    /**
     * Sets the location of the album.
     * @param location the location of the album
     */
    public void setLocation(String location);

    /**
     * Returns the link of the album.
     * @return the link of the album
     */
    public String getLink();

    /**
     * Sets the link of the album.
     * @param link the link of the album
     */
    public void setLink(String link);

    /**
     * Returns the cover photo of the album.
     * @return the cover photo of the album
     */
    public String getCoverPhoto();

    /**
     * Sets the cover photo of the album.
     * @param coverPhoto the cover photo of the album
     */
    public void setCoverPhoto(String coverPhoto);

    /**
     * Returns the privacy setting of the album.
     * @return the privacy setting of the album
     */
    public String getPrivacy();

    /**
     * Sets the privacy setting of the album.
     * @param privacy the privacy setting of the album
     */
    public void setPrivacy(String privacy);

    /**
     * Returns the photos count of the album.
     * @return the photos count of the album
     */
    public String getCount();

    /**
     * Sets the photos count of the album.
     * @param count the photos count of the album
     */
    public void setCount(String count);

    /**
     * Returns the album type.
     * @return the album type
     */
    public String getType();

    /**
     * Sets the album type.
     * @param type the album type.
     */
    public void setType(String type);

    /**
     * Returns the created time of the album.
     * @return the created time of the album
     */
    public String getCreatedTime();

    /**
     * Sets the created time of the album.
     * @param createdTime the created time of the album
     */
    public void setCreatedTime(String createdTime);

    /**
     * Returns the updated time of the album.
     * @return the updated time of the album
     */
    public String getUpdatedTime();

    /**
     * Sets the updated time of the album.
     * @param updatedTime the updated time of the album
     */
    public void setUpdatedTime(String updatedTime);

    /**
     * Returns the can upload permission of the album.
     * @return the can upload permission of the album
     */
    public boolean getCanUpload();

    /**
     * Sets the can upload permission of the album.
     * @param canUpload the can upload permission of the album
     */
    public void setCanUpload(boolean canUpload);
}
