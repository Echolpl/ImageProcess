package application;

import java.io.File;
import java.io.IOException;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.CvType;

public class MyModel {
	public Mat m_matImage = null; // ��Ųɼ��׶εĹ�һ��ǰ��ͼ�����ʶ��׶εĲ���ͼ��ľ���
	public Mat m_matNormImage = null; // ͼ��ɼ��׶λ���ʶ��׶ι�һ���Ժ������ͼ�����
	public Mat m_matTrainingImage = null; // ѵ������������M*N
	public Mat m_matEigenFace = null; // ����������(�����)
	public Mat m_matEigenTrainingSamples = null; // ѵ�����������������ռ��ͶӰ����(�����)
	public Mat matMeanVector = null;
	public Mat eigen_train_sample = null;
	public Mat EigenFace = null;
	int numPerson; // �˵ĸ���
	int numSamplePerPerson; // ÿ���˵�ѵ��������

	MyModel() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		numPerson = 8; // 40����
		numSamplePerPerson = 5; // ÿ��5������
	}

	/** ����ѵ����������������m_matTrainingImage������ */
	public void ReadTrainingSampleFiles(File file) { // fileΪѵ���������е�һ����������
		String fileParent = "file:///" + file.getParent();// ���ѵ��������Ŀ¼
		String filePath = "file:///" + file.getPath();
		filePath = filePath.replace('\\', '/'); // ʹ�ñ����ļ���URL·��
		fileParent = fileParent.replace('\\', '/'); // ʹ�ñ����ļ���URL·��
		Image sample = new Image(filePath); // ����һ��ѵ������
		// ����ѵ����������numPerson*numSamplePerPerson�У�sample.getWidth()*sample.getHeight()��
		int img_w = (int) sample.getWidth(); // ѵ���������
		int img_h = (int) sample.getHeight(); // ѵ�������߶�
		m_matTrainingImage = new Mat(numPerson * numSamplePerPerson, img_w * img_h, CvType.CV_32F);
		for (int i = 1; i <= numPerson; i++) {// ��i����
			for (int j = 1; j <= numSamplePerPerson; j++) // ��j������
			{
				float[] gray = new float[img_w * img_h]; // ���1����ͼ���������صĻҶ�ֵ
				String s = fileParent + "/s" + i + "_" + j + ".bmp"; // ͼ��si_j.bmp·��
				Image si_j = new Image(s);// ��ȡͼ��si_j.bmp
				PixelReader si_jPixelReader = si_j.getPixelReader(); // ͼ��si_j.bmp���ض�����
				int n = 0;
				for (int x = 0; x < img_w; x++) { // ���б���ͼ��
					for (int y = 0; y < img_h; y++) {
						Color color = si_jPixelReader.getColor(x, y);
						gray[n] = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0);
						n++;
					}
				}
				m_matTrainingImage.put(((i - 1) * numSamplePerPerson + j - 1), 0, gray);
			}
		}
	}

	/** �ú�����m_matImage����˫��������м��κͻҶȹ�һ�������������m_matNormImage�� */
	public void NormalizeImage(int lefteye_x, int lefteye_y, int righteye_x, int righteye_y) {
		if (m_matImage.width() / 300 > m_matImage.height() / 300) {
			lefteye_x = lefteye_x * m_matImage.width() / 300;
			lefteye_y = lefteye_y * m_matImage.width() / 300;
			righteye_x = righteye_x * m_matImage.width() / 300;
			righteye_y = righteye_y * m_matImage.width() / 300;
		} else {
			lefteye_x = lefteye_x * m_matImage.height() / 300;
			lefteye_y = lefteye_y * m_matImage.height() / 300;
			righteye_x = righteye_x * m_matImage.height() / 300;
			righteye_y = righteye_y * m_matImage.height() / 300;
		}
		int centerX = (lefteye_x - righteye_x + 1) / 2 + righteye_x;
		int centerY = (lefteye_y - righteye_y + 1) / 2 + righteye_y;
		int x_center = m_matImage.width() / 2;
		int y_center = m_matImage.height() / 2;
		Point centerPoint = new Point(centerX, centerY);

		Mat matImage = new Mat((int) (m_matImage.height()), (int) (m_matImage.width()), CvType.CV_32F);
		double e = Math.atan(((float) (lefteye_y - righteye_y) / (lefteye_x - righteye_x)));
		matImage = Imgproc.getRotationMatrix2D(centerPoint, e * (180 / Math.PI), 1.0);
		System.out.println(e);

		centerX = (int) ((centerX - x_center) * Math.cos(e) - (centerY - y_center) * Math.sin(e) + x_center);
		centerY = (int) ((centerX - x_center) * Math.sin(e) + (centerY - y_center) * Math.cos(e) + y_center);
		m_matNormImage = m_matImage.clone();
		Imgproc.warpAffine(m_matImage, m_matNormImage, matImage, m_matNormImage.size(), Imgproc.INTER_NEAREST);

		CutImage(lefteye_x, lefteye_y, righteye_x, righteye_y);
	}

	public void CutImage(int lefteye_x, int lefteye_y, int righteye_x, int righteye_y) {
		int x_center = m_matImage.width() / 2;
		int y_center = m_matImage.height() / 2;

		int centerX = (lefteye_x - righteye_x + 1) / 2 + righteye_x;
		int centerY = (lefteye_y - righteye_y + 1) / 2 + righteye_y;
		System.out.println(centerX + " " + centerY + " " + x_center + " " + y_center);
		double distance1 = Math.pow(Math.pow(lefteye_x - righteye_x, 2) + Math.pow(lefteye_y - righteye_y, 2), 0.5);
		double distance2 = Math.pow(Math.pow(x_center - centerX, 2) + Math.pow(y_center - centerY, 2), 0.5);
		int x = 0, y = 0;
		x = (int) (centerX - distance1);
		y = (int) (centerY - (distance1) / 2);

		System.out.println(x + " " + y + " " + distance1 + " " + distance2);
		Rect rect = new Rect(x, y, (int) distance1 * 2, (int) distance1 * 2);

		m_matNormImage = new Mat(m_matNormImage, rect);
		Mat matImage = new Mat(48, 48, CvType.CV_32F);
		Imgproc.resize(m_matNormImage, m_matNormImage, matImage.size());
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_8UC1, 255, 0);
		Imgproc.equalizeHist(m_matNormImage, m_matNormImage);
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_32F, 1.0 / 255, 0);
	}

	/** ��ѵ������������m_matTrainingImage�ĵõ����������󣬱�����������m_matEigenFace�� */
	public void CalculateEigenFaceMat() {
		int count = 0, countNum = 0;
		Mat normTrainFaceMat = new Mat(m_matTrainingImage.height(), m_matTrainingImage.width(), CvType.CV_32F);
		float[] trainingImgData = new float[m_matTrainingImage.height() * m_matTrainingImage.width()];
		m_matTrainingImage.get(0, 0, trainingImgData);
		// normTrainFaceMat(i,:)= trainFaceMat(i,:)-meanFaceMat;
		// ����ѵ��������ƽ��ֵ����meanFaceMat

		matMeanVector = new Mat(m_matTrainingImage.height(), 1, CvType.CV_32F);// ������ƽ��ֵ�õ��ľ���
		matMeanVector.create(1, m_matTrainingImage.cols(), CvType.CV_32F);
		for (int i = 0; i < m_matTrainingImage.cols(); i++) {
			Mat colRange = m_matTrainingImage.colRange(i, i + 1);
			Scalar mean = Core.mean(colRange);
			matMeanVector.put(0, i, mean.val[0]);
		}
		System.out.println("����ѵ��������ƽ��ֵ����meanFaceMat:" + matMeanVector);
		System.out.println("����ѵ��������ƽ��ֵ����meanFaceMat:" + matMeanVector.dump());

		// �����񻯺��ѵ����������normTrainFaceMat�������СΪM��N
		float[] matMeanVectorData = new float[m_matTrainingImage.height() * m_matTrainingImage.width()];
		matMeanVector.get(0, 0, matMeanVectorData);
		for (int i = 0; i < m_matTrainingImage.height(); i++) {

			for (int j = 0; j < m_matTrainingImage.width(); j++) {
				normTrainFaceMat.put(i, j, trainingImgData[i * m_matTrainingImage.width() + j] - matMeanVectorData[i]);
			}
			countNum++;
		}
		System.out.println("�����񻯺��ѵ����������normTrainFaceMat:" + normTrainFaceMat);
		// ����normTrainFaceMat* normTrainFaceMat�����������ֵ������������
		Mat EigenValues = new Mat();
		Mat EigenVectors = new Mat();
		Mat EigenMat = new Mat();
		Core.mulTransposed(normTrainFaceMat, EigenMat, false);
		System.out.println("ѵ����������normTrainFaceMat��ת�þ���˻�:" + EigenMat);
		// Core.gemm(normTrainFaceMat, normTrainFaceMat.t(), 1.0, new Mat(), 0,
		// EigenMat);
		Core.eigen(EigenMat, EigenValues, EigenVectors);

		System.out.println("EigenValues:" + EigenValues.dump());
		System.out.println("EigenVectors:" + EigenVectors.dump());
		// ����ֵĬ�ϰ���������

		// �������ռ���н�ά

		float[] value = new float[EigenValues.height() * EigenValues.width()];
		EigenValues.get(0, 0, value);

		int m = 0;
		float ValuesSum = 0, sum = 0;
		for (int i = 0; i < EigenValues.rows(); i++) {
			ValuesSum = ValuesSum + value[i];
		}
		for (int i = 0; i < EigenValues.rows(); i++) {
			sum = sum + value[i];
			if (sum / ValuesSum >= 0.9) {
				m = i;
				break;
			}
		}
		m++;
		System.out.println("m=" + m);
		Mat eig_vec = new Mat();// ��Ž�ά��ľ���
		eig_vec.create(EigenVectors.rows(), m, CvType.CV_32F);
		for (int y = 0; y < m; y++) {
			for (int x = 0; x < EigenVectors.rows(); x++) {
				double a = EigenVectors.get(x, y)[0];
				double b = EigenValues.get(y, 0)[0];
				double c = a / Math.sqrt(b);
				eig_vec.put(x, y, c);

			}
		}
		// System.out.println("eig_vec:" + eig_vec.dump());
		// ѵ���������������ռ�
		EigenFace = new Mat();
		Core.gemm(normTrainFaceMat.t(), eig_vec, 1.0, new Mat(), 0, EigenFace);
		// System.out.println("EigenFace:" + EigenFace);
		// System.out.println("EigenFace:" + EigenFace.dump());
		// ѵ���������������ռ��ͶӰ
		eigen_train_sample = new Mat();
		Core.gemm(normTrainFaceMat, EigenFace, 1.0, new Mat(), 0, eigen_train_sample);
		System.out.println("eigen_train_sample:" + eigen_train_sample);
		System.out.println("����ѵ����ɣ�����");

	}

	/** ��������m_matEigenFace�����Զ������ļ���ʽ�����ڵ�ǰ�ļ����� */
	public void SaveEigenFaceMat() {
		// ʹ��java�Ķ������������ļ�����
	}

	/** �ӵ�ǰĿ¼��������������Ķ������ļ����������뵽m_matEigenFace�ļ��� */
	public void ReadEigenFaceMatFromFile() throws IOException {

	}

	/** ��������m_matEigenTrainingSamples�����Զ������ļ���ʽ�����ڵ�ǰ�ļ����� */
	public void SaveEigenTrainingSamplesMat() {
		// ʹ��java�Ķ������������ļ�����
	}

	/** �ӵ�ǰĿ¼����ѵ�����������������ռ��ͶӰ����Ķ������ļ����������뵽m_matEigenTrainingSamples�ļ��� */
	public void ReadEigenTrainingSamplesMatFromFile() throws IOException {

	}

	/** ������ӽ�������������������:ʹ�ò���������m_matNormImage����������������m_matEigenFace���м��� */
	/** �ú������������������ӽ����������� */
	public int GetSimilarFaceMat() {
		System.out.println("GetSimilarFaceMat");
		// ����������չ��Ϊ1��N����testFaceMat NΪһ��ͼ���������ذ�������������ֵ

		float[] m_matNormImageData = new float[m_matNormImage.cols() * m_matNormImage.rows()];
		m_matNormImage.get(0, 0, m_matNormImageData);

		Mat testFaceMat = new Mat(1, m_matNormImage.width() * m_matNormImage.height(), CvType.CV_32F);
		for (int i = 0; i < m_matNormImage.height(); i++) {
			for (int j = 0; j < m_matNormImage.width(); j++) {
				testFaceMat.put(0, j * m_matNormImage.height() + i, m_matNormImageData[i * m_matNormImage.width() + j]);
			}
		}
		float[] testFaceMatData = new float[testFaceMat.width()];
		testFaceMat.get(0, 0, testFaceMatData);
		// testFaceMat.put(0, 0, );
		System.out.println("testFaceMat" + testFaceMat);
		// ��һ��ͼƬƽ��ֵ����meanFaceMat ǰ���Ѿ����
		float[] matMeanVectorData = new float[m_matNormImage.height() * m_matNormImage.width()];
		matMeanVector.get(0, 0, matMeanVectorData);

		System.out.println("matMeanVector" + matMeanVector);
		System.out.println("testFaceMat" + testFaceMat);

		// �����񻯺��ʶ����������normTestFaceMat��
		Mat normTestFaceMat = new Mat(1, m_matNormImage.height() * m_matNormImage.width(), CvType.CV_32F);
		for (int i = 0; i < m_matNormImage.height() * m_matNormImage.width(); i++) {
			normTestFaceMat.put(0, i, testFaceMatData[i] - matMeanVectorData[i]);
		}
		System.out.println("normTestFaceMat" + normTestFaceMat);
		// ����normTrainFaceMat* normTrainFaceMat�����������ֵ������������

		// ����ֵĬ�ϰ���������

		// �������ռ���н�ά

		// ѵ���������������ռ�

		// ѵ���������������ռ��ͶӰ

		// EigenFaceǰ���Ѿ������
		Mat eigen_test_sample = new Mat();

		Core.gemm(normTestFaceMat, EigenFace, 1.0, new Mat(), 0, eigen_test_sample);

		System.out.println("eigen_train_sample:" + eigen_train_sample);
		System.out.println("eigen_test_sample:" + eigen_test_sample);
		System.out.println("eigen_train_sample:" + eigen_train_sample.dump());
		System.out.println("eigen_test_sample:" + eigen_test_sample.dump());

		float[] eigen_train_sampleData = new float[eigen_train_sample.height() * eigen_train_sample.width()];
		eigen_train_sample.get(0, 0, eigen_train_sampleData);
		double min_distance = 1000;
		int min_i = 0;
		for (int i = 0; i < eigen_train_sample.height(); i++) {
			Mat eigenTrainSample = new Mat(1, eigen_train_sample.width(), CvType.CV_32F);
			for (int j = 0; j < eigen_train_sample.width(); j++) {
				eigenTrainSample.put(0, j, eigen_train_sampleData[i * eigen_train_sample.width() + j]);
			}
			eigenTrainSample = eigen_train_sample.row(i).clone();
			double distance = Core.norm(eigen_test_sample, eigenTrainSample);
			if (min_distance > distance) {
				min_distance = distance;
				min_i = i;
			}
		}
		System.out.println("����(ͼƬλ��):" + min_i + "\n");
		System.out.println("��Ӧŷʽ����:" + min_distance + "\n");
		if (min_distance > 30) {
			return -1;
		} else
			return min_i;
	}

	/** ������matImg�е�ֵ��ͼ��ķ�ʽ��� */
	public WritableImage GetImageFromMatrix(Mat matImg) {
		if (matImg != null) {
			WritableImage wImage = new WritableImage(matImg.width(), matImg.height());
			// �õ�����д����
			PixelWriter pixelWriter = wImage.getPixelWriter();
			if (matImg.channels() == 1) { // ��ͨ��ͼ��
				float[] gray = new float[matImg.height() * matImg.width()];
				matImg.get(0, 0, gray);
				// ����ͼ�����ÿ�����أ�����д�뵽Ŀ��ͼ��
				for (int y = 0; y < matImg.height(); y++) {
					for (int x = 0; x < matImg.width(); x++) {
						int pixelIndex = y * matImg.width() + x;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}
			} else if (matImg.channels() == 3) { // 3ͨ��ͼ��
				// ����Դͼ��ÿ�����أ�����д�뵽Ŀ��ͼ��
				for (int y = 0; y < matImg.height(); y++) {
					for (int x = 0; x < matImg.width(); x++) {
						int[] gray = new int[3];
						matImg.get(y, x, gray);
						Color color = Color.rgb(gray[2], gray[1], gray[0]);
						pixelWriter.setColor(x, y, color);
					}
				}
			}
			return wImage;
		}
		return null;
	}

	/** ��ͼ��image�����������m_matImage������ */
	public Mat GetGrayImgMatFromImage(Image image) {
		if (image != null) {
			PixelReader pixelReader = image.getPixelReader();
			m_matImage = new Mat((int) (image.getHeight()), (int) (image.getWidth()), CvType.CV_32F);
			// ����Դͼ��ÿ�����أ�����д�뵽Ŀ��ͼ�����
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					Color color = pixelReader.getColor(x, y);
					float gray = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0);
					m_matImage.put(y, x, gray);
				}
			}
		}
		return m_matImage;
	}

	/** �ӻҶ�ͼ�񼯺�(ѵ��������������������)�����г�ȡһ�л�һ����ɶ�άͼ�� */
	public WritableImage GetImageFromGrayImagesMatrix(Mat matImages, // ͼ�񼯺Ͼ���
			int imgWidth, // ��άͼ��Ŀ��
			int index, // ȡ���л����е�����
			boolean byRow) // trueΪ��ȡһ�У�falseΪ��ȡһ��
	{
		if (matImages != null || matImages.channels() != 1) {
			int widthMat = matImages.width();
			int heightMat = matImages.height();
			if (byRow == true) { // ���г�ȡ
				int imgHeight = widthMat / imgWidth; // ��άͼ��߶�
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				// �õ�����д����
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[widthMat];
				matImages.get(index, 0, gray);
				// ����ͼ��ÿ������
				for (int x = 0; x < wImage.getWidth(); x++) {
					for (int y = 0; y < wImage.getHeight(); y++) {
						int pixelIndex = x * imgHeight + y;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}
				return wImage;
			} else { // ���г�ȡ
				int imgHeight = heightMat / imgWidth; // ��άͼ��߶�
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				// �õ�����д����
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[heightMat];
				Mat matTImages = matImages.t(); // ����ת��
				matTImages.get(index, 0, gray);
				// ����ͼ��ÿ������
				for (int x = 0; x < wImage.getWidth(); x++) {
					for (int y = 0; y < wImage.getHeight(); y++) {
						int pixelIndex = x * imgHeight + y;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}
				return wImage;
			}
		}
		return null;
	}
}
